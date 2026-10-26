package com.ysmragdoll.platform;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class YsmPlatform {
    public static final String RENDERER_REGISTRY = "com.elfmcys.yesstevemodel.oooO0Ooo0oOOOOooOoo00Oo0";
    public static final String MESH_RENDERER = "com.elfmcys.yesstevemodel.OoooOoo0o0000O0o00o0O0Oo";
    private YsmPlatform() {}
    public static List<String> rendererRegistries() {
        return List.of("org.openysm.client.renderer.RendererManager", RENDERER_REGISTRY);
    }
    public static List<String> meshRenderers() {
        return List.of("org.openysm.geckolib3.geo.NativeModelRenderer", MESH_RENDERER);
    }
    public static boolean loaded() {
        return ModList.get().isLoaded("openysm") || ModList.get().isLoaded("yes_steve_model");
    }
    public static Object modelState(Player player, ClassLoader loader) throws ReflectiveOperationException {
        for (String[] binding : new String[][] {
                {"org.openysm.capability.forge.PlayerCapabilityImpl", "get"},
                {"com.elfmcys.yesstevemodel.OoooO0OO0000O00oo0Oo00OO", "oOoo00O0o0oO0o0oO00OO0O0"}
        }) {
            try {
                Class<?> state = Class.forName(binding[0], false, loader);
                Method getter = state.getDeclaredMethod(binding[1], Player.class);
                Object result = getter.invoke(null, player);
                return result instanceof Optional<?> optional ? optional.orElse(null) : null;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
