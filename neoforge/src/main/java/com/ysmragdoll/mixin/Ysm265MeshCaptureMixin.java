package com.ysmragdoll.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ysmragdoll.client.OpenYsmModelAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.OoooOoo0o0000O0o00o0O0Oo", remap = false)
abstract class Ysm265MeshCaptureMixin {
    private static final String DRAW_METHOD =
            "oOo0OO0O0o000OO0O000oo0o(Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                    + "Lcom/elfmcys/yesstevemodel/O0OOo0OoOoo0o0o0oO000o00;[F[FIIIIFFFF)V";

    @Inject(method = DRAW_METHOD, at = @At("HEAD"), remap = false,
            cancellable = true, require = 1)
    private static void ysmragdoll$captureSnapshot(VertexConsumer consumer, PoseStack.Pose pose,
                                                    @Coerce Object mesh,
                                                    float[] boneTransforms, float[] secondaryState,
                                                    int textureIndex, int renderPartMask,
                                                    int packedLight, int packedOverlay,
                                                    float red, float green, float blue, float alpha,
                                                    CallbackInfo callback) {
        if (OpenYsmModelAdapter.onYsmMeshRender(pose, mesh, boneTransforms, secondaryState,
                textureIndex, renderPartMask, packedLight, packedOverlay,
                red, green, blue, alpha)) {
            callback.cancel();
        }
    }
}
