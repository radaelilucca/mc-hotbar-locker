package com.radaeli.hotbarlocker.neoforge.network;

import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LockStatePayload(int mask) implements CustomPacketPayload {
    public static final Type<LockStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HotbarLocker.MOD_ID, "lock_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockStatePayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, LockStatePayload::mask, LockStatePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
