package com.ysmragdoll.compatibility;

import com.ysmragdoll.platform.YsmPlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Inspect the supplied binary without initializing its renderer, native code or GPU. */
@EnabledIfSystemProperty(named = "ysmragdoll.openysmJar", matches = ".+")
class OpenYsmForgeBindingsTest {
    private static final String NATIVE_RENDERER =
            "com/elfmcys/yesstevemodel/geckolib3/geo/NativeModelRenderer";

    @Test
    void registryAndPlayerStateBindingsExistInTheActualOpenYsmJar() throws Exception {
        try (var jar = input()) {
            String registry = YsmPlatform.rendererRegistries().stream()
                    .filter(name -> jar.getEntry(name.replace('.', '/') + ".class") != null)
                    .findFirst().orElseThrow();
            var node = read(jar.getInputStream(jar.getEntry(registry.replace('.', '/') + ".class")));
            var getter = node.methods.stream().filter(m -> m.name.equals("getPlayerRenderer")
                    && (m.access & Opcodes.ACC_STATIC) != 0).findFirst().orElseThrow();
            assertEquals(0, Type.getArgumentTypes(getter.desc).length);
            String playerRenderer = Type.getReturnType(getter.desc).getInternalName();
            var player = read(jar.getInputStream(jar.getEntry(playerRenderer + ".class")));
            assertTrue(player.methods.stream().anyMatch(m -> m.desc.equals(
                    "(Lnet/minecraft/world/entity/player/Player;FFLcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V")));
            var capability = read(jar.getInputStream(jar.getEntry(
                    "com/elfmcys/yesstevemodel/forge/capability/PlayerCapabilityProvider.class")));
            assertTrue(capability.fields.stream().anyMatch(f -> f.name.equals("PLAYER_CAP")
                    && (f.access & Opcodes.ACC_STATIC) != 0
                    && f.desc.equals("Lnet/minecraftforge/common/capabilities/Capability;")));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void captureSelectorsCoverTheActualTexturedDrawCall() throws Exception {
        try (var jar = input()) {
            assertTrue(YsmPlatform.meshRenderers().contains(NATIVE_RENDERER.replace('/', '.')));
            var renderer = read(jar.getInputStream(jar.getEntry(NATIVE_RENDERER + ".class")));
            var mixin = read(getClass().getResourceAsStream(
                    "/com/ysmragdoll/mixin/OpenYsmMeshCaptureMixin.class"));
            Set<String> selectors = new HashSet<>();
            for (var method : mixin.methods) {
                if (method.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : method.visibleAnnotations) {
                    if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
                    for (int i = 0; i < annotation.values.size(); i += 2) {
                        if (annotation.values.get(i).equals("method")) {
                            selectors.addAll((List<String>) annotation.values.get(i + 1));
                        }
                    }
                }
            }
            assertEquals(2, selectors.size(), "Both legacy and textured entries need a capture hook");
            for (String selector : selectors) {
                assertTrue(renderer.methods.stream().anyMatch(m -> (m.name + m.desc).equals(selector)), selector);
            }
            var caller = read(jar.getInputStream(jar.getEntry(
                    "com/elfmcys/yesstevemodel/geckolib3/geo/IGeoRenderer.class")));
            boolean checked = false;
            for (var method : caller.methods) {
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.owner.equals(NATIVE_RENDERER)
                            && call.name.equals("renderMesh") && Type.getArgumentTypes(call.desc).length == 14) {
                        assertTrue(selectors.contains(call.name + call.desc),
                                "The textured entry used by the player renderer must be intercepted");
                        checked = true;
                    }
                }
            }
            assertTrue(checked, "Test binary must exercise OpenYSM's direct textured draw path");
        }
        try (var config = getClass().getResourceAsStream("/ysmragdoll.mixins.json")) {
            assertNotNull(config);
            assertTrue(new String(config.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .contains("OpenYsmMeshCaptureMixin"), "The hook must be registered in the shipped config");
        }
    }

    private static ZipFile input() throws Exception {
        return new ZipFile(Path.of(System.getProperty("ysmragdoll.openysmJar")).toFile());
    }

    @Test
    void deathHandoffCoversMinecraftAndOpenYsmDirectRendering() throws Exception {
        var dispatcher = read(getClass().getResourceAsStream(
                "/net/minecraft/client/renderer/entity/EntityRenderDispatcher.class"));
        var dispatcherMixin = read(getClass().getResourceAsStream(
                "/com/ysmragdoll/mixin/DeathRenderHandoffMixin.class"));
        Set<String> dispatcherSelectors = selectors(dispatcherMixin);
        assertTrue(dispatcher.methods.stream().anyMatch(m -> dispatcherSelectors.contains(m.name + m.desc)),
                "Minecraft's entity dispatcher must have a matching render entry");
        assertTrue(dispatcherSelectors.stream().anyMatch(s -> s.startsWith("m_114384_(")),
                "Forge production must use the verified 1.20.1 SRG render name");
        try (var jar = input()) {
            var playerRenderer = read(jar.getInputStream(jar.getEntry(
                    "com/elfmcys/yesstevemodel/client/renderer/CustomPlayerRenderer.class")));
            var playerMixin = read(getClass().getResourceAsStream(
                    "/com/ysmragdoll/mixin/OpenYsmPlayerRenderMixin.class"));
            Set<String> playerSelectors = selectors(playerMixin);
            assertEquals(1, playerSelectors.size());
            assertTrue(playerRenderer.methods.stream().anyMatch(m -> playerSelectors.contains(m.name + m.desc)),
                    "OpenYSM's direct/first-person renderer must also support the handoff");
        }
        try (var config = getClass().getResourceAsStream("/ysmragdoll.mixins.json")) {
            String json = new String(config.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(json.contains("DeathRenderHandoffMixin"));
            assertTrue(json.contains("OpenYsmPlayerRenderMixin"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> selectors(ClassNode mixin) {
        Set<String> result = new HashSet<>();
        for (var method : mixin.methods) {
            if (method.visibleAnnotations == null) continue;
            for (var annotation : method.visibleAnnotations) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if (annotation.values.get(i).equals("method")) {
                        result.addAll((List<String>) annotation.values.get(i + 1));
                    }
                }
            }
        }
        return result;
    }

    private static ClassNode read(InputStream input) throws Exception {
        assertNotNull(input, "Required class must be present");
        try (input) {
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }
}
