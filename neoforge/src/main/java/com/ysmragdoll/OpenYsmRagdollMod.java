package com.ysmragdoll;

import com.mojang.logging.LogUtils;
import com.ysmragdoll.client.ClientBootstrap;
import com.ysmragdoll.config.YsmRagdollConfig;
import com.ysmragdoll.network.RagdollNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(OpenYsmRagdollMod.MOD_ID)
public final class OpenYsmRagdollMod {
    public static final String MOD_ID = "ysmragdoll";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenYsmRagdollMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, YsmRagdollConfig.SPEC, "ysmragdoll-client.toml");
        modBus.addListener(RagdollNetwork::register);
        if (FMLEnvironment.dist.isClient()) ClientBootstrap.initialize(modBus);
    }
}
