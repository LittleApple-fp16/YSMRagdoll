package com.ysmragdoll.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ysmragdoll.client.OpenYsmModelAdapter;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer", remap = false)
abstract class OpenYsmMeshCaptureMixin {
    private static final String ARGUMENTS =
            "(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                    + "Lcom/elfmcys/yesstevemodel/geckolib3/geo/render/built/GeoModel;[F[FIIIIFFFF";

    @Inject(method = "renderMesh" + ARGUMENTS + ")V", at = @At("HEAD"),
            remap = false, cancellable = true, require = 0)
    private static void ysmragdoll$captureSnapshot(VertexConsumer consumer, PoseStack.Pose pose,
                                                    @Coerce Object mesh,
                                                    float[] boneTransforms, float[] secondaryState,
                                                    int textureIndex, int renderPartMask,
                                                    int packedLight, int packedOverlay,
                                                    float red, float green, float blue, float alpha,
                                                    CallbackInfo callback) {
        if (OpenYsmModelAdapter.onYsmMeshRender(pose, mesh, boneTransforms, secondaryState,
                textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderMesh" + ARGUMENTS + "Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD"), remap = false, cancellable = true, require = 0)
    private static void ysmragdoll$captureTexturedSnapshot(VertexConsumer consumer, PoseStack.Pose pose,
                                                            @Coerce Object mesh,
                                                            float[] boneTransforms, float[] secondaryState,
                                                            int textureIndex, int renderPartMask,
                                                            int packedLight, int packedOverlay,
                                                            float red, float green, float blue, float alpha,
                                                            ResourceLocation texture,
                                                            CallbackInfo callback) {
        if (OpenYsmModelAdapter.onYsmMeshRender(pose, mesh, boneTransforms, secondaryState,
                textureIndex, renderPartMask, packedLight, packedOverlay,
                red, green, blue, alpha, texture)) {
            callback.cancel();
        }
    }
}
