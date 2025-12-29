package com.leclowndu93150.thick_air.network;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.ThickAir;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.leclowndu93150.thick_air.capability.AirBubblePositions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record ChunkAirQualityPayload(ChunkPos chunkPos, Map<BlockPos, AirQualityLevel> airBubblePositions, Mode mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChunkAirQualityPayload> TYPE =
            new CustomPacketPayload.Type<>(ThickAir.id("chunk_air_quality"));

    public static final StreamCodec<FriendlyByteBuf, ChunkAirQualityPayload> STREAM_CODEC = StreamCodec.of(
            ChunkAirQualityPayload::encode,
            ChunkAirQualityPayload::decode
    );

    private static void encode(FriendlyByteBuf buf, ChunkAirQualityPayload payload) {
        buf.writeLong(payload.chunkPos.toLong());
        buf.writeVarInt(payload.airBubblePositions.size());
        for (Map.Entry<BlockPos, AirQualityLevel> entry : payload.airBubblePositions.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeByte(entry.getValue().ordinal());
        }
        buf.writeEnum(payload.mode);
    }

    private static ChunkAirQualityPayload decode(FriendlyByteBuf buf) {
        ChunkPos chunkPos = new ChunkPos(buf.readLong());
        int size = buf.readVarInt();
        Map<BlockPos, AirQualityLevel> positions = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            AirQualityLevel quality = AirQualityLevel.values()[buf.readByte()];
            positions.put(pos, quality);
        }
        Mode mode = buf.readEnum(Mode.class);
        return new ChunkAirQualityPayload(chunkPos, positions, mode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ChunkAirQualityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getChunkSource().hasChunk(payload.chunkPos.x, payload.chunkPos.z)) {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(payload.chunkPos.x, payload.chunkPos.z);
                if (chunk != null) {
                    AirBubblePositions capability = chunk.getData(ModRegistry.AIR_BUBBLE_POSITIONS);
                    Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositions();
                    switch (payload.mode) {
                        case REPLACE -> {
                            airBubblePositions.clear();
                            airBubblePositions.putAll(payload.airBubblePositions);
                        }
                        case ADD -> airBubblePositions.putAll(payload.airBubblePositions);
                        case REMOVE -> payload.airBubblePositions.keySet().forEach(airBubblePositions::remove);
                    }
                }
            }
        });
    }

    public enum Mode {
        REPLACE, REMOVE, ADD
    }
}
