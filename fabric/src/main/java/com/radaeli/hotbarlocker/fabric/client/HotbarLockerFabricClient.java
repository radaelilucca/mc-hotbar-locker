package com.radaeli.hotbarlocker.fabric.client;

import com.radaeli.hotbarlocker.HotbarLocker;
import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.fabric.network.LockStatePayload;
import com.radaeli.hotbarlocker.fabric.network.ToggleLockPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HotbarLockerFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotbarLocker.MOD_ID + "/client");
    private static final KeyMapping TOGGLE_LOCK = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.hotbarlocker.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.hotbarlocker"));

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(LockStatePayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null) return;
            HotbarLockService.setMask(context.client().player, payload.mask());
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_LOCK.consumeClick()) {
                if (client.player == null) continue;
                ClientPlayNetworking.send(new ToggleLockPayload(client.player.getInventory().selected));
            }
        });
        LOGGER.info("{} client adapter initialized on Fabric", HotbarLocker.MOD_ID);
    }
}
