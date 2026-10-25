package com.ysmragdoll.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.List;

/** Forge-specific access to YSM; shared rendering never links Fabric to Forge APIs. */
public final class YsmPlatform {
    public static final String RENDERER_REGISTRY = "com.elfmcys.yesstevemodel.OOoO00ooO00OOO00O0o0000O";
    public static final String MESH_RENDERER = "com.elfmcys.yesstevemodel.ooOOo000OOO0ooO0oo0ooooO";

    private YsmPlatform() {}

    public static List<String> rendererRegistries() {
        return List.of(RENDERER_REGISTRY,
                "com.elfmcys.yesstevemodel.client.renderer.RendererManager");
    }

    public static List<String> meshRenderers() {
        return List.of(MESH_RENDERER,
                "com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer");
    }

    public static boolean loaded() {
        return ModList.get().isLoaded("yes_steve_model");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object modelState(Player player, ClassLoader loader) throws ReflectiveOperationException {
        for (String[] binding : new String[][] {
                {"com.elfmcys.yesstevemodel.forge.capability.PlayerCapabilityProvider", "PLAYER_CAP"},
                {"com.elfmcys.yesstevemodel.O0OooOo0oOOoOoOoOooO000o", "Oo0Oo0o00O00Oo0OOoOOoooo"}
        }) {
            try {
                Class<?> provider = Class.forName(binding[0], false, loader);
                Field field = provider.getDeclaredField(binding[1]);
                field.setAccessible(true);
                Object handle = field.get(null);
                if (handle instanceof Capability capability) {
                    return player.getCapability(capability).resolve().orElse(null);
                }
            } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                // Official YSM and OpenYSM use different names for the same state.
            }
        }
        return null;
    }
}
