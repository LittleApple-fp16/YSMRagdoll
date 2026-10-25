package com.ysmragdoll.event;

import com.ysmragdoll.OpenYsmRagdollMod;
import com.ysmragdoll.network.PlayerDeathSnapshot;
import com.ysmragdoll.network.RagdollNetwork;
import com.ysmragdoll.YsmRagdollLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


@EventBusSubscriber(modid = OpenYsmRagdollMod.MOD_ID)
public final class ServerDeathEvents {
    private ServerDeathEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        YsmRagdollLog.info("服务端捕获玩家死亡: " + player.getGameProfile().getName()
                + " / " + player.getUUID());
        RagdollNetwork.sendDeathSnapshot(player, PlayerDeathSnapshot.from(player));
    }
}
