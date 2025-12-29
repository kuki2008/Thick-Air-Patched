package com.leclowndu93150.thick_air.handler;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.ThickAir;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.leclowndu93150.thick_air.capability.AirBubblePositions;
import com.leclowndu93150.thick_air.network.ChunkAirQualityPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AirBubbleTracker {
    // Performance: Use ConcurrentHashMap instead of synchronized HashSet
    private static final Set<ChunkPos> CHUNKS_TO_SCAN = ConcurrentHashMap.newKeySet();
    private static final List<Map.Entry<ChunkPos, BlockPos>> CHUNK_SCANNING_PROGRESS = Collections.synchronizedList(new LinkedList<>());

    public static void onBlockStateChange(ServerLevel level, BlockPos pos, BlockState oldBlockState, BlockState newBlockState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk != null && !oldBlockState.is(newBlockState.getBlock())) {
            AirBubblePositions capability = chunk.getData(ModRegistry.AIR_BUBBLE_POSITIONS);
            if (AirQualityLevel.getAirQualityFromBlock(oldBlockState) != null) {
                // Need to remove this
                AirQualityLevel removed = capability.getAirBubblePositions().remove(pos);
                if (removed == null) {
                    ThickAir.LOGGER.debug("Didn't remove any air bubbles at {}", pos);
                } else {
                    chunk.setUnsaved(true);
                    PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos,
                            new ChunkAirQualityPayload(chunkPos, Map.of(pos, removed), ChunkAirQualityPayload.Mode.REMOVE));
                }
            }

            AirQualityLevel airQualityLevel = AirQualityLevel.getAirQualityFromBlock(newBlockState);
            if (airQualityLevel != null) {
                AirQualityLevel clobbered = capability.getAirBubblePositions().put(pos, airQualityLevel);
                if (clobbered != null) {
                    ThickAir.LOGGER.debug("Clobbered air bubble at {}: {}", pos, clobbered);
                }
                chunk.setUnsaved(true);
                PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos,
                        new ChunkAirQualityPayload(chunkPos, Map.of(pos, airQualityLevel), ChunkAirQualityPayload.Mode.ADD));
            }
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            CHUNKS_TO_SCAN.add(chunk.getPos());
            CHUNK_SCANNING_PROGRESS.add(Map.entry(chunk.getPos(), getChunkStartingPosition(chunk)));
        }
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            CHUNKS_TO_SCAN.remove(chunk.getPos());
        }
    }

    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        LevelChunk chunk = event.getChunk();
        AirBubblePositions capability = chunk.getData(ModRegistry.AIR_BUBBLE_POSITIONS);
        Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositionsView();
        PacketDistributor.sendToPlayer(player,
                new ChunkAirQualityPayload(chunk.getPos(), airBubblePositions, ChunkAirQualityPayload.Mode.REPLACE));
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            CHUNKS_TO_SCAN.clear();
            CHUNK_SCANNING_PROGRESS.clear();
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (!CHUNK_SCANNING_PROGRESS.isEmpty()) {
            synchronized (CHUNK_SCANNING_PROGRESS) {
                if (CHUNK_SCANNING_PROGRESS.isEmpty()) return;

                ListIterator<Map.Entry<ChunkPos, BlockPos>> iterator = CHUNK_SCANNING_PROGRESS.listIterator();
                Map.Entry<ChunkPos, BlockPos> entry = iterator.next();
                ChunkPos chunkPos = entry.getKey();

                if (CHUNKS_TO_SCAN.contains(chunkPos)) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                    if (chunk != null) {
                        AirBubblePositions capability = chunk.getData(ModRegistry.AIR_BUBBLE_POSITIONS);

                        capability.setSkipCountLeft(8);
                        HashMap<BlockPos, AirQualityLevel> airBubblePositions = new HashMap<>();
                        BlockPos blockPos = collectAirQualityPositions(chunk, entry.getValue(), airBubblePositions);
                        boolean markDirty = false;

                        if (entry.getValue().equals(getChunkStartingPosition(chunk))) {
                            capability.getAirBubblePositions().clear();
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos,
                                    new ChunkAirQualityPayload(chunkPos, airBubblePositions, ChunkAirQualityPayload.Mode.REPLACE));
                            markDirty = true;
                        } else if (!airBubblePositions.isEmpty()) {
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos,
                                    new ChunkAirQualityPayload(chunkPos, airBubblePositions, ChunkAirQualityPayload.Mode.ADD));
                            markDirty = true;
                        }

                        if (markDirty) {
                            chunk.setUnsaved(true);
                        }

                        if (blockPos != null) {
                            iterator.set(Map.entry(chunkPos, blockPos));
                            return;
                        }
                    } else {
                        return;
                    }
                }
                iterator.remove();
                CHUNKS_TO_SCAN.remove(chunkPos);
            }
        }
    }

    private static BlockPos getChunkStartingPosition(LevelChunk chunk) {
        int posX = chunk.getPos().getMinBlockX();
        int posY = chunk.getMinBuildHeight();
        int posZ = chunk.getPos().getMinBlockZ();
        return new BlockPos(posX, posY, posZ);
    }

    @Nullable
    private static BlockPos collectAirQualityPositions(LevelChunk chunk, BlockPos startingPosition, Map<BlockPos, AirQualityLevel> airBubbleEntries) {
        int minX = chunk.getPos().getMinBlockX();
        int minY = chunk.getMinBuildHeight();
        int minZ = chunk.getPos().getMinBlockZ();
        int startX = startingPosition.getX() - minX;
        int startY = startingPosition.getY();
        int startZ = startingPosition.getZ() - minZ;
        int iterations = 0;

        for (int dx = startX; dx < 16; dx++, startX = 0) {
            for (int dz = startZ; dz < 16; dz++, startZ = 0) {
                int posX = minX + dx;
                int posZ = minZ + dz;
                int maxY = chunk.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, posX, posZ);
                for (int posY = startY; posY < maxY; posY++, startY = minY, iterations++) {
                    BlockPos blockPos = new BlockPos(posX, posY, posZ);
                    // Limit iterations per tick for performance
                    if (iterations >= 98304) {
                        return blockPos;
                    }
                    BlockState blockState = chunk.getBlockState(blockPos);
                    AirQualityLevel airQualityLevel = AirQualityLevel.getAirQualityFromBlock(blockState);
                    if (airQualityLevel != null) {
                        airBubbleEntries.put(blockPos, airQualityLevel);
                    }
                }
            }
        }
        return null;
    }
}
