package com.radaeli.hotbarlocker.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.hotbarlocker.HotbarLocker;
import com.radaeli.hotbarlocker.neoforge.network.HotbarLockerNeoForgeNetwork;
import com.radaeli.hotbarlocker.neoforge.network.ToggleLockPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HotbarLocker.MOD_ID)
public final class HotbarLockerNeoForgeEvents {
    private HotbarLockerNeoForgeEvents() { }
    @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HotbarLockerNeoForgeNetwork.sync(player);
    }
    @EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private static final KeyMapping TOGGLE = new KeyMapping("key.hotbarlocker.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.hotbarlocker");
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            while (TOGGLE.consumeClick()) if (client.player != null) {
                PacketDistributor.sendToServer(new ToggleLockPayload(client.player.getInventory().selected));
            }
        }
        @EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
        public static final class Registration {
            @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
        }
    }
}
