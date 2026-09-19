package com.radaeli.hotbarlocker.fabric;

import com.radaeli.hotbarlocker.HotbarLocker;
import com.radaeli.hotbarlocker.fabric.network.HotbarLockerFabricNetwork;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HotbarLockerFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotbarLocker.MOD_ID);

    @Override
    public void onInitialize() {
        HotbarLockerFabricNetwork.initializeServer();
        LOGGER.info("{} common bootstrap initialized on Fabric", HotbarLocker.MOD_ID);
    }
}
