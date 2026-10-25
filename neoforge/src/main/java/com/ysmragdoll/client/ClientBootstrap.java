package com.ysmragdoll.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public final class ClientBootstrap {
    private ClientBootstrap() {}
    public static void initialize(IEventBus bus) {
        bus.addListener(RagdollKeyMappings::register);
        bus.addListener(ClientBootstrap::reload);
    }
    private static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager ->
                ClientRagdollManager.clear("客户端资源重新加载"));
    }
}
