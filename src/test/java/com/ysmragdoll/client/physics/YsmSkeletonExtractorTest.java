package com.ysmragdoll.client.physics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YsmSkeletonExtractorTest {
    @TempDir Path root;

    private record NativeBone(int nameId) {}
    private record BakedBone(int parent) {}

    private Field parentField(List<?> bones, String name) throws Exception {
        Method choose = YsmSkeletonExtractor.class.getDeclaredMethod("chooseParentField", List.class, List.class);
        choose.setAccessible(true);
        Field field = bones.get(0).getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (Field) choose.invoke(null, List.of(field), bones);
    }

    @Test void nativeNameIdIsNeverTreatedAsHierarchy() throws Exception {
        assertNull(parentField(List.of(new NativeBone(180), new NativeBone(291)), "nameId"));
        assertNull(parentField(List.of(new NativeBone(1), new NativeBone(2), new NativeBone(0)), "nameId"));
    }

    @Test void hierarchyMustBeValidAndAcyclicButMayUseForwardReferences() throws Exception {
        assertNotNull(parentField(List.of(new BakedBone(-1), new BakedBone(2), new BakedBone(0)), "parent"));
        assertNull(parentField(List.of(new BakedBone(-1), new BakedBone(2), new BakedBone(1)), "parent"));
        assertNull(parentField(List.of(new BakedBone(-1), new BakedBone(-2)), "parent"));
    }

    @Test void fileHierarchyRestoresNativeParentLinksWithoutChangingRuntimeOrder() throws Exception {
        Path directory = Files.createDirectories(root.resolve("custom/test-model"));
        Files.writeString(directory.resolve("ysm.json"), """
                {"files":{"player":{"model":{"main":"main.json"}}}}
                """);
        Files.writeString(directory.resolve("main.json"), """
                {"minecraft:geometry":[{"bones":[
                  {"name":"AllBody","pivot":[0,16,0]},
                  {"name":"LeftArm","parent":"AllBody","pivot":[4,20,0]},
                  {"name":"sleeve","parent":"LeftArm","pivot":[4,20,0]},
                  {"name":"Sleeve","parent":"AllBody","pivot":[6,18,0]}
                ]}]}
                """);
        List<RagdollDefinition.Bone> runtime = List.of(
                bone(0, "sleeve", new Vector3f(-4,20,0), new Matrix4f()),
                bone(1, "AllBody", new Vector3f(0,16,0), new Matrix4f()),
                bone(2, "LeftArm", new Vector3f(-4,20,0), new Matrix4f()),
                bone(3, "Sleeve", new Vector3f(-6,18,0), new Matrix4f()));
        List<RagdollDefinition.Bone> restored = OpenYsmGeometryLoader.enrich(root, runtime, List.of());
        assertEquals(2, restored.get(0).parentIndex());
        assertEquals(-1, restored.get(1).parentIndex());
        assertEquals(1, restored.get(2).parentIndex());
        assertEquals(1, restored.get(3).parentIndex(), "Distinct YSM names must not merge during lookup");
        assertEquals(runtime.get(0).name(), restored.get(0).name());
        List<RagdollDefinition.Bone> wrongModel = List.of(
                bone(0, "AllBody", new Vector3f(0,32,0), new Matrix4f()));
        assertSame(wrongModel, OpenYsmGeometryLoader.enrich(root, wrongModel, List.of("test-model")),
                "Common bone names must not bind a different model's hierarchy");
    }

    @Test void hiddenVariantDoesNotWinOverVisibleBodyOrCreateLimbBodies() throws Exception {
        List<RagdollDefinition.Bone> bones = List.of(
                bone(0, "MAllBody", new Vector3f(), new Matrix4f().scale(0)),
                bone(1, "AllBody", new Vector3f(), new Matrix4f()),
                bone(2, "LeftArm", new Vector3f(), new Matrix4f().scale(0)),
                bone(3, "RightArm", new Vector3f(), new Matrix4f()));
        Method mapping = YsmSkeletonExtractor.class.getDeclaredMethod("mapParts", List.class);
        mapping.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<RagdollDefinition.Role, RagdollDefinition.Part> parts =
                (Map<RagdollDefinition.Role, RagdollDefinition.Part>) mapping.invoke(null, bones);
        assertEquals(1, parts.get(RagdollDefinition.Role.BODY).boneIndex());
        assertFalse(parts.containsKey(RagdollDefinition.Role.LEFT_UPPER_ARM));
        assertTrue(parts.containsKey(RagdollDefinition.Role.RIGHT_UPPER_ARM));
    }

    @Test void headMeshSizesCollisionButHeadContainerDrivesHairAndAccessories() throws Exception {
        List<RagdollDefinition.Bone> bones = List.of(
                bone(0, "AllBody", new Vector3f(), new Matrix4f()),
                new RagdollDefinition.Bone(1, "AllHead", 0, new Vector3f(0,24,0),
                        new Vector3f(-0.01F), new Vector3f(0.01F), new Matrix4f()),
                new RagdollDefinition.Bone(2, "Head", 1, new Vector3f(0,24,0),
                        new Vector3f(-0.2F,1.5F,-0.2F), new Vector3f(0.2F,1.9F,0.2F), new Matrix4f()),
                new RagdollDefinition.Bone(3, "Hair", 1, new Vector3f(0,24,0),
                        new Vector3f(-0.3F,1.5F,-0.3F), new Vector3f(0.3F,2,0.3F), new Matrix4f()));
        Method mapping = YsmSkeletonExtractor.class.getDeclaredMethod("mapParts", List.class);
        mapping.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<RagdollDefinition.Role, RagdollDefinition.Part> parts =
                (Map<RagdollDefinition.Role, RagdollDefinition.Part>) mapping.invoke(null, bones);
        var head = parts.get(RagdollDefinition.Role.HEAD);
        assertEquals(1, head.boneIndex());
        assertEquals(1.7F, head.center().y, 0.0001F);
        assertTrue(head.halfExtents().x > 0.1F, "Use the head mesh, not the tiny container marker");
    }

    private static RagdollDefinition.Bone bone(int index, String name, Vector3f pivot, Matrix4f global) {
        return new RagdollDefinition.Bone(index, name, -1, pivot,
                new Vector3f(Float.POSITIVE_INFINITY), new Vector3f(Float.NEGATIVE_INFINITY), global);
    }
}
