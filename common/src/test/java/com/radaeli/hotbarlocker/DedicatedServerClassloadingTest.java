package com.radaeli.hotbarlocker;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Checks compiled references, including descriptors of compiler-generated lambdas. */
class DedicatedServerClassloadingTest {
    private final Path classes = Path.of(System.getProperty("hotbarlocker.neoClasses"));

    @Test
    void sharedNetworkAndBootstrapDoNotReferenceClientClasses() throws Exception {
        assertServerSafe(classes.resolve("com/radaeli/hotbarlocker/neoforge/HotbarLockerNeoForge.class"));
        assertServerSafe(classes.resolve("com/radaeli/hotbarlocker/neoforge/HotbarLockerNeoForgeEvents.class"));
        Path network = classes.resolve("com/radaeli/hotbarlocker/neoforge/network");
        try (var paths = Files.walk(network)) {
            var compiled = paths.filter(path -> path.toString().endsWith(".class")).toList();
            assertFalse(compiled.isEmpty(), "Network classes must have been compiled");
            for (Path path : compiled) assertServerSafe(path);
        }
    }

    @Test
    void detectorRejectsActualClientBytecode() {
        assertThrows(AssertionError.class, () -> assertServerSafe(
                classes.resolve("com/radaeli/hotbarlocker/mixin/client/GuiMixin.class")));
    }

    private static void assertServerSafe(Path path) throws Exception {
        // JVM constant-pool class names are stored as UTF-8 ASCII strings.
        String bytecode = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
        for (String prefix : new String[] {"net/minecraft/client/", "com/mojang/blaze3d/", "net/neoforged/neoforge/client/"}) {
            assertFalse(bytecode.contains(prefix), () -> path + " references physical-client code: " + prefix);
        }
    }
}
