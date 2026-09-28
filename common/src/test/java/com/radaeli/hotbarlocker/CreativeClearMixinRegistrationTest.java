package com.radaeli.hotbarlocker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Guards the client-only registration for Creative's Clear Inventory protection. */
class CreativeClearMixinRegistrationTest {
    @Test
    void registersCreativeClearProtectionAsClientOnlyMixin() throws Exception {
        InputStream config = getClass().getResourceAsStream("/hotbarlocker.mixins.json");
        assertTrue(config != null, "Mixin configuration must be available to tests");
        String json = new String(config.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"client\""));
        assertTrue(json.contains("client.CreativeModeInventoryScreenMixin"));
        assertTrue(!json.substring(json.indexOf("\"mixins\""), json.indexOf("\"client\"")).contains("CreativeModeInventoryScreenMixin"));
    }
}
