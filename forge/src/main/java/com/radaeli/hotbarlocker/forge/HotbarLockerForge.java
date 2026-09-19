package com.radaeli.hotbarlocker.forge;

import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HotbarLocker.MOD_ID)
public final class HotbarLockerForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotbarLocker.MOD_ID);

    public HotbarLockerForge() {
        HotbarLockerForgeNetwork.initialize();
        LOGGER.info("{} common bootstrap initialized on Forge", HotbarLocker.MOD_ID);
    }
}
