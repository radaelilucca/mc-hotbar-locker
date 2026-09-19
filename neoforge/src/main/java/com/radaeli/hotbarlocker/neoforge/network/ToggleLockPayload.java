package com.radaeli.hotbarlocker.neoforge.network;

import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleLockPayload(int slot) implements CustomPacketPayload {
    public static final Type<ToggleLockPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HotbarLocker.MOD_ID, "toggle_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleLockPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ToggleLockPayload::slot, ToggleLockPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
