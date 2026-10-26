package com.ysmragdoll.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysmragdoll.client.ClientRagdollManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.openysm.client.renderer.CustomPlayerRenderer", remap = false)
abstract class OpenYsmPlayerRenderMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/player/Player;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void ysmragdoll$hideReplacedPlayer(Player player, float yaw, float partialTick,
                                               PoseStack pose, MultiBufferSource buffers, int light,
                                               CallbackInfo callback) {
        if (ClientRagdollManager.shouldHideReplacedPlayer(player)) callback.cancel();
    }
}
