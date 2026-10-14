package com.radaeli.hotbarlocker.forge;

import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.LoggerFactory;

@Mod(HotbarLocker.MOD_ID)
public final class HotbarLockerForge {
    public HotbarLockerForge() {
        HotbarLockerForgeNetwork.initialize();
        LoggerFactory.getLogger(HotbarLocker.MOD_ID).info("hotbarlocker common bootstrap initialized on Forge 1.20.1");
    }
}
