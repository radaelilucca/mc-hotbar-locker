package com.radaeli.hotbarlocker;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the distributable, including reobfuscation; source compilation alone is insufficient. */
class Forge1201ArtifactTest {
    private JarFile artifact() throws Exception { return new JarFile(System.getProperty("hotbarlocker.jar")); }
    private static byte[] bytes(JarFile jar, String name) throws Exception {
        assertNotNull(jar.getJarEntry(name), "Missing artifact entry: " + name);
        return jar.getInputStream(jar.getJarEntry(name)).readAllBytes();
    }
    private static String text(JarFile jar, String name) throws Exception {
        return new String(bytes(jar, name), StandardCharsets.UTF_8);
    }

    @Test void artifactDeclaresExactGameVersionAndProductionMixins() throws Exception {
        try (var jar = artifact()) {
            String metadata = text(jar, "META-INF/mods.toml");
            assertTrue(metadata.contains("versionRange=\"[1.20.1]\""));
            assertTrue(metadata.contains("versionRange=\"[47.4.0,48)\""));
            assertFalse(metadata.contains("${"), "Unexpanded metadata");
            assertEquals("hotbarlocker.mixins.json", jar.getManifest().getMainAttributes().getValue("MixinConfigs"));
            String mixins = text(jar, "hotbarlocker.mixins.json");
            assertTrue(mixins.contains("JAVA_17"));
            assertTrue(mixins.contains("hotbarlocker.refmap.json"));
            assertTrue(mixins.contains("ToolboxEquipPacket1201Mixin"));
            assertFalse(mixins.contains("\"client.GuiMixin\""));
            assertTrue(text(jar, "pack.mcmeta").contains("15"));
        }
    }

    @Test void allVanillaInjectionTargetsHaveProductionMappings() throws Exception {
        try (var jar = artifact()) {
            String refmap = text(jar, "hotbarlocker.refmap.json");
            for (String target : new String[] {"setPickedItem", "pickSlot", "getSuitableHotbarSlot",
                    "addAdditionalSaveData", "readAdditionalSaveData", "slotClicked", "renderHotbar", "pickBlock"}) {
                assertTrue(refmap.contains("\"" + target + "\": \"Lnet/minecraft/"), "Missing refmap target: " + target);
            }
            // Shadow fields need the AP's extra SRG mappings, not just a refmap.
            String creative = new String(bytes(jar, "com/radaeli/hotbarlocker/mixin/client/CreativeModeInventoryScreenMixin.class"), StandardCharsets.ISO_8859_1);
            assertFalse(creative.contains("destroyItemSlot"), "Creative shadow was not reobfuscated");
            assertTrue(creative.contains("f_98512_"), "Unexpected Creative shadow mapping");
        }
    }

    @Test void translationsAreIdenticalAndEveryClassRunsOnJava17() throws Exception {
        try (var jar = artifact()) {
            for (String language : new String[] {"en_us", "pt_br"}) {
                String name = "assets/hotbarlocker/lang/" + language + ".json";
                assertArrayEquals(Files.readAllBytes(Path.of(System.getProperty("hotbarlocker.commonResources"), name)), bytes(jar, name));
            }
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                assertFalse(entry.getName().contains("hotbarlocker/tests/"), "Validation code leaked into shipped mod");
                if (entry.getName().endsWith(".class")) {
                    byte[] compiled = bytes(jar, entry.getName());
                    assertEquals(61, ((compiled[6] & 255) << 8) | (compiled[7] & 255), entry.getName());
                }
            }
        }
    }

    @Test void serverBootstrapAndTransportNeverLinkMinecraftClientClasses() throws Exception {
        try (var jar = artifact()) {
            for (String name : new String[] {"HotbarLockerForge", "HotbarLockerForgeEvents", "HotbarLockerForgeNetwork",
                    "HotbarLockerForgeNetwork$Toggle", "HotbarLockerForgeNetwork$State"}) {
                String compiled = new String(bytes(jar, "com/radaeli/hotbarlocker/forge/" + name + ".class"), StandardCharsets.ISO_8859_1);
                assertFalse(compiled.contains("net/minecraft/client/"), name);
                assertFalse(compiled.contains("com/mojang/blaze3d/"), name);
            }
        }
    }
}
