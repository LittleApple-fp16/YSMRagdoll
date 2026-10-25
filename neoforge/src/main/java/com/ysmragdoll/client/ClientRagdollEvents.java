package com.ysmragdoll.client;

import com.ysmragdoll.OpenYsmRagdollMod;
import com.ysmragdoll.YsmRagdollLog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = OpenYsmRagdollMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRagdollEvents {
    private ClientRagdollEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GravityGunController.update(1.0F);
        ClientRagdollManager.tick();
        Minecraft minecraft = Minecraft.getInstance();
        boolean requested = false;
        while (RagdollKeyMappings.OPEN_SETTINGS.consumeClick()) {
            requested = true;
        }
        if (requested && !RagdollKeyMappings.OPEN_SETTINGS.isUnbound()
                && minecraft.level != null && minecraft.player != null && minecraft.screen == null) {
            minecraft.setScreen(new YsmRagdollSettingsScreen(null));
            YsmRagdollLog.info("通过快捷键打开设置页面");
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        long startedAt = System.nanoTime();
        ClientRagdollManager.render(event.getPoseStack(), event.getCamera().getPosition(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        ClientIntensiveLogger.recordFrame();
        ClientPerformanceLogger.recordFrame(System.nanoTime() - startedAt,
                ClientRagdollManager.ragdollCount(), ClientRagdollManager.physicsRagdollCount());
    }

    @SubscribeEvent
    public static void onUseItem(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && GravityGunController.isArmed()) {
            if (event.getHand() == InteractionHand.MAIN_HAND) GravityGunController.start();
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null || event.getHand() != InteractionHand.MAIN_HAND
                || !event.isUseItem() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (ClientRagdollManager.removeLookingAt(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (GravityGunController.scroll(event.getScrollDeltaY())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPerformanceLogger.reset();
        ClientRagdollManager.clear("客户端断开连接");
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientPerformanceLogger.reset();
            ClientRagdollManager.clear("客户端世界卸载或切换维度");
        }
    }
}
