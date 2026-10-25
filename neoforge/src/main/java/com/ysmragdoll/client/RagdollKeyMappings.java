package com.ysmragdoll.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ysmragdoll.OpenYsmRagdollMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.bus.api.SubscribeEvent;


@OnlyIn(Dist.CLIENT)
public final class RagdollKeyMappings {
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.ysmragdoll.open_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.ysmragdoll");

    private RagdollKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }
}
