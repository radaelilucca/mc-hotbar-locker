package com.radaeli.hotbarlocker.neoforge;

import com.radaeli.hotbarlocker.HotbarLocker;
import com.radaeli.hotbarlocker.neoforge.network.HotbarLockerNeoForgeNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HotbarLocker.MOD_ID)
public final class HotbarLockerNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotbarLocker.MOD_ID);

    public HotbarLockerNeoForge(IEventBus modBus) {
        modBus.addListener(HotbarLockerNeoForgeNetwork::register);
        LOGGER.info("{} common bootstrap initialized on NeoForge", HotbarLocker.MOD_ID);
    }
}
