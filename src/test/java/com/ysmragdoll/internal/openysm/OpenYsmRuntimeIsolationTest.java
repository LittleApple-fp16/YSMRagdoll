package com.ysmragdoll.internal.openysm;

import com.ysmragdoll.internal.openysm.algorithms.MT19937;
import com.ysmragdoll.internal.openysm.algorithms.XChaCha20;
import com.ysmragdoll.internal.openysm.resource.YSMBinaryDeserializer;
import com.ysmragdoll.internal.openysm.resource.YSMFolderDeserializer;
import com.ysmragdoll.internal.openysm.zstd.ZstdUtil;
import org.junit.jupiter.api.Test;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class OpenYsmRuntimeIsolationTest {
    @Test
    void relocatedParserReadsAnimationTypeThroughPrivateHelper() {
        // Legacy format 1: no metadata blob or geometry, one empty arm animation,
        // then empty texture/metadata fields. This calls YSMFolderDeserializer in
        // the relocated bytecode and constructs RawYsmModel's nested types.
        byte[] binary = new byte[64];
        binary[2] = 1;
        binary[3] = 2;
        binary[4] = 1;
        var model = new YSMBinaryDeserializer(binary, 1).deserialize();
        assertEquals(1, model.formatVersion);
        assertEquals(2, model.mainEntity.animationFiles.get("arm").animType);
        assertEquals("com.ysmragdoll.internal.openysm.resource.pojo.RawYsmModel",
                model.getClass().getName());
    }

    @Test
    void relocatedDecompressorAndCipherRemainUsable() throws Exception {
        byte[] input = "OpenYSM private parser compatibility".repeat(32)
                .getBytes(StandardCharsets.UTF_8);
        // Standard Zstd frame containing one final raw block, independent of the
        // embedded compressor (the model loader only uses decompression).
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), ZstdUtil.decompress(
                HexFormat.of().parseHex("28b52ffd200529000068656c6c6f")));
        byte[] key = new byte[32];
        byte[] iv = new byte[24];
        byte[] encrypted = new XChaCha20(key, iv, 20).processBytes(input, 0, input.length);
        assertFalse(java.util.Arrays.equals(input, encrypted));
        assertArrayEquals(input, new XChaCha20(key, iv, 20)
                .processBytes(encrypted, 0, encrypted.length));
        // OpenYSM implements the 64-bit MT19937 variant.
        assertEquals(Long.parseUnsignedLong("14514284786278117030"),
                new MT19937(5489L).extract_number());
    }

    @Test
    void modPackagesResolveAlongsideYsmAndJer() throws Exception {
        Set<String> packages = new HashSet<>();
        addPackages(packages, YSMBinaryDeserializer.class);
        addPackages(packages, YSMFolderDeserializer.class);
        assertTrue(packages.contains("com.ysmragdoll.internal.openysm.resource"));
        assertTrue(packages.contains("com.ysmragdoll.internal.openysm.algorithms"));
        assertTrue(packages.stream().noneMatch(p -> p.startsWith("com.elfmcys.yesstevemodel")
                || p.startsWith("rip.ysm")), packages.toString());
        assertDoesNotThrow(() -> resolve(packages));

        // Control: reintroducing the original split package must reproduce the
        // module-resolution failure even with an unrelated receiving mod (JER).
        packages.add("com.elfmcys.yesstevemodel.resource");
        assertThrows(ResolutionException.class, () -> resolve(packages));
    }

    private static void addPackages(Set<String> packages, Class<?> type) throws Exception {
        Path path = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(path)) {
            try (var entries = Files.walk(path)) {
                entries.filter(p -> p.toString().endsWith(".class"))
                        .forEach(p -> addPackage(packages, path.relativize(p).toString().replace('\\', '/')));
            }
        } else {
            try (var archive = new ZipFile(path.toFile())) {
                archive.stream().map(java.util.zip.ZipEntry::getName)
                        .filter(n -> n.endsWith(".class")).forEach(n -> addPackage(packages, n));
            }
        }
    }

    private static void addPackage(Set<String> packages, String name) {
        int separator = name.lastIndexOf('/');
        if (separator > 0) packages.add(name.substring(0, separator).replace('/', '.'));
    }

    private static Configuration resolve(Set<String> packages) {
        var ysm = ModuleDescriptor.newAutomaticModule("yes_steve_model").packages(Set.of(
                "com.elfmcys.yesstevemodel.resource", "com.elfmcys.yesstevemodel.resource.pojo",
                "rip.ysm.algorithms", "rip.ysm.security", "rip.ysm.zstd")).build();
        var ragdoll = ModuleDescriptor.newAutomaticModule("ysmragdoll").packages(packages).build();
        var jer = ModuleDescriptor.newModule("jeresources")
                .requires("yes_steve_model").requires("ysmragdoll").build();
        Map<String, ModuleReference> modules = Set.of(ysm, ragdoll, jer).stream()
                .map(d -> new ModuleReference(d, null) {
                    @Override
                    public ModuleReader open() {
                        throw new UnsupportedOperationException("Resolution only needs descriptors");
                    }
                }).collect(Collectors.toMap(m -> m.descriptor().name(), Function.identity()));
        ModuleFinder finder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(modules.get(name));
            }

            @Override
            public Set<ModuleReference> findAll() {
                return Set.copyOf(modules.values());
            }
        };
        return ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), Set.of("jeresources"));
    }
}
