package com.ysmragdoll.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class MeshCaptureTest {
    @Test
    void bothLegacyAndTexturedReplaySignaturesAreRecognized() {
        assertEquals(13, OpenYsmModelAdapter.findMeshRenderMethod(LegacyRenderer.class, Object.class)
                .getParameterCount());
        assertEquals(14, OpenYsmModelAdapter.findMeshRenderMethod(TexturedRenderer.class, Object.class)
                .getParameterCount());
        assertNull(OpenYsmModelAdapter.findMeshRenderMethod(Object.class, Object.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void texturedCaptureFreezesArraysAndRetainsTheSubmittedTexture() throws Exception {
        PoseStack pose = new PoseStack();
        var contextType = Class.forName(OpenYsmModelAdapter.class.getName() + "$CaptureContext");
        var constructor = contextType.getDeclaredConstructor(PoseStack.Pose.class, boolean.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(pose.last(), true);
        Field active = field(OpenYsmModelAdapter.class, "ACTIVE_CAPTURE");
        ThreadLocal<Object> capture = (ThreadLocal<Object>) active.get(null);
        float[] bones = {1, 2, 3};
        float[] secondary = {4, 5, 6};
        ResourceLocation texture = ResourceLocation.tryParse("ysmragdoll:captured");
        assertFalse(OpenYsmModelAdapter.onYsmMeshRender(pose.last(), new Object(), bones,
                secondary, 0, 0, 0, 0, 1, 1, 1, 1, texture),
                "Normal player rendering must not be cancelled");
        assertFalse(OpenYsmModelAdapter.isCapturing());
        capture.set(context);
        try {
            assertTrue(OpenYsmModelAdapter.isCapturing());
            assertTrue(OpenYsmModelAdapter.onYsmMeshRender(pose.last(), new Object(), bones,
                    secondary, 0, 0, 0, 0, 1, 1, 1, 1, texture));
            Object draft = field(contextType, "draft").get(context);
            bones[0] = 99;
            secondary[0] = 99;
            assertArrayEquals(new float[]{1, 2, 3}, (float[]) field(draft.getClass(), "boneTransforms").get(draft));
            assertArrayEquals(new float[]{4, 5, 6}, (float[]) field(draft.getClass(), "secondaryState").get(draft));
            assertEquals(texture, field(draft.getClass(), "texture").get(draft));
        } finally {
            capture.remove();
        }
        assertFalse(OpenYsmModelAdapter.isCapturing());
    }

    @Test
    void frozenSnapshotReplaysThroughTheTexturedOverload() throws Exception {
        var type = Class.forName(OpenYsmModelAdapter.class.getName() + "$ModelSnapshot");
        var constructor = type.getDeclaredConstructor(Object.class, float[].class, float[].class,
                int.class, int.class, int.class, float.class, float.class, float.class, float.class,
                Matrix4f.class, Matrix3f.class, ResourceLocation.class, RenderType.class);
        constructor.setAccessible(true);
        ResourceLocation texture = ResourceLocation.tryParse("ysmragdoll:frozen");
        Object snapshot = constructor.newInstance(new Object(), new float[]{1}, new float[]{2},
                0, 0, 0, 1F, 1F, 1F, 1F, new Matrix4f(), new Matrix3f(), texture, null);
        Field draw = field(OpenYsmModelAdapter.class, "ysmMeshRenderMethod");
        Object previous = draw.get(null);
        Method replay = OpenYsmModelAdapter.class.getDeclaredMethod("renderSnapshot", type,
                PoseStack.class, MultiBufferSource.class, int.class);
        replay.setAccessible(true);
        draw.set(null, OpenYsmModelAdapter.findMeshRenderMethod(TexturedRenderer.class, Object.class));
        try {
            TexturedRenderer.submittedTexture = null;
            assertEquals(true, replay.invoke(null, snapshot, new PoseStack(),
                    (MultiBufferSource) ignored -> new DiscardingVertexConsumer(), 0));
            assertEquals(texture, TexturedRenderer.submittedTexture);
        } finally {
            draw.set(null, previous);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    static class LegacyRenderer {
        public static void draw(VertexConsumer consumer, PoseStack.Pose pose, Object mesh,
                                float[] bones, float[] state, int texture, int mask, int light,
                                int overlay, float red, float green, float blue, float alpha) {}
    }

    static class TexturedRenderer {
        static ResourceLocation submittedTexture;

        public static void draw(VertexConsumer consumer, PoseStack.Pose pose, Object mesh,
                                float[] bones, float[] state, int texture, int mask, int light,
                                int overlay, float red, float green, float blue, float alpha) {
            fail("The textured overload must take precedence");
        }

        public static void draw(VertexConsumer consumer, PoseStack.Pose pose, Object mesh,
                                float[] bones, float[] state, int texture, int mask, int light,
                                int overlay, float red, float green, float blue, float alpha,
                                ResourceLocation location) {
            submittedTexture = location;
        }
    }
}
