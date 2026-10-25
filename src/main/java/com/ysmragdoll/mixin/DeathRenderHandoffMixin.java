package com.ysmragdoll.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysmragdoll.client.ClientRagdollManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
abstract class DeathRenderHandoffMixin {
    // Forge 1.20.1 uses SRG method names in production; no refmap is needed here.
    @Inject(method = {
            "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "m_114384_(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void ysmragdoll$hideReplacedPlayer(Entity entity, double x, double y, double z,
                                               float yaw, float partialTick, PoseStack pose,
                                               MultiBufferSource buffers, int light,
                                               CallbackInfo callback) {
        if (entity instanceof Player player && ClientRagdollManager.shouldHideReplacedPlayer(player)) {
            callback.cancel();
        }
    }
}
