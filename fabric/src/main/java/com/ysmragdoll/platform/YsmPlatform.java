package com.ysmragdoll.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.Properties;
import java.util.List;

/** Version-specific renderer bindings are generated from the selected build target. */
public final class YsmPlatform {
    private static final Properties BINDINGS = loadBindings();
    public static final String RENDERER_REGISTRY = BINDINGS.getProperty("registry");
    public static final String MESH_RENDERER = BINDINGS.getProperty("mesh");

    private YsmPlatform() {}

    public static List<String> rendererRegistries() {
        return List.of(RENDERER_REGISTRY);
    }

    public static List<String> meshRenderers() {
        return List.of(MESH_RENDERER);
    }

    private static Properties loadBindings() {
        Properties result = new Properties();
        try (var stream = YsmPlatform.class.getResourceAsStream("/ysmragdoll-platform.properties")) {
            if (stream == null) throw new IllegalStateException("Missing YSM platform bindings");
            result.load(stream);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return result;
    }

    public static boolean loaded() {
        return FabricLoader.getInstance().isModLoaded("yes_steve_model");
    }

    public static Object modelState(Player player, ClassLoader loader) throws ReflectiveOperationException {
        // YSM supplies its own capability interface through EntityMixin on Fabric.
        // Its client render state stores the selected model ID in an inherited String field.
        // Select by the Optional's type, avoiding version-specific obfuscated names.
        for (Method method : player.getClass().getMethods()) {
            if (!isModelStateGetter(method)) continue;
            Object result = method.invoke(player);
            if (result instanceof Optional<?> state && state.isPresent()) return state.get();
        }
        return null;
    }

    public static boolean isModelStateGetter(Method method) {
        try {
            if (!method.getDeclaringClass().getName().startsWith("com.elfmcys.yesstevemodel.")
                    || Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                    || method.getReturnType() != Optional.class
                    || !(method.getGenericReturnType() instanceof ParameterizedType optional)
                    || !(optional.getActualTypeArguments()[0] instanceof Class<?> state)) return false;
            for (Class<?> type = state; type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) return true;
                }
            }
        } catch (TypeNotPresentException | LinkageError ignored) {
            // Some YSM getters reference optional integrations (e.g. Touhou Little Maid).
            // Their absent classes must not prevent reading the player's own model state.
        }
        return false;
    }
}
