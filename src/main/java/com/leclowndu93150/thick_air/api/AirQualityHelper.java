package com.leclowndu93150.thick_air.api;

import com.leclowndu93150.thick_air.Config;
import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.capability.AirBubblePositions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;

public final class AirQualityHelper {

    private AirQualityHelper() {}

    public static AirQualityLevel getAirQualityAtLocation(LivingEntity entity) {
        return getAirQualityAtLocation(entity.level(), entity.getEyePosition());
    }

    public static AirQualityLevel getAirQualityAtLocation(Level level, Vec3 location) {
        BlockState blockAtEyes = level.getBlockState(BlockPos.containing(location));
        AirQualityLevel airQualityAtEyes = AirQualityLevel.getAirQualityAtEyes(blockAtEyes);
        if (airQualityAtEyes != null) return airQualityAtEyes;

        // Let's throw the player a bone and say the best air quality wins
        AirQualityLevel bestAirBubbleQuality = null;
        ChunkPos chunkAtCenter = new ChunkPos(BlockPos.containing(location));

        // Max radius for air provider is 32, so that means we check two chunks on each side
        // Performance: use squared distance to avoid sqrt
        outer:
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                ChunkPos posInChunk = new ChunkPos(chunkAtCenter.x + x, chunkAtCenter.z + z);
                LevelChunk chunk = level.getChunkSource().getChunkNow(posInChunk.x, posInChunk.z);
                if (chunk == null) continue;

                AirBubblePositions capability = chunk.getData(ModRegistry.AIR_BUBBLE_POSITIONS);
                Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositionsView();

                for (Map.Entry<BlockPos, AirQualityLevel> entry : airBubblePositions.entrySet()) {
                    BlockPos blockPos = entry.getKey();
                    AirQualityLevel airQualityLevel = entry.getValue();
                    Objects.requireNonNull(airQualityLevel, "air quality level is null");

                    if (bestAirBubbleQuality == null || airQualityLevel.isBetterThan(bestAirBubbleQuality)) {
                        // Performance: use squared distance to avoid sqrt
                        double distanceSq = location.distanceToSqr(
                                blockPos.getX() + 0.5,
                                blockPos.getY() + 0.5,
                                blockPos.getZ() + 0.5
                        );
                        double radiusSq = airQualityLevel.getAirProviderRadius() * airQualityLevel.getAirProviderRadius();

                        if (distanceSq < radiusSq) {
                            if (airQualityLevel == AirQualityLevel.GREEN) {
                                // Early exit - can't get better than GREEN
                                return AirQualityLevel.GREEN;
                            } else {
                                bestAirBubbleQuality = airQualityLevel;
                            }
                        }
                    }
                }
            }
        }

        if (bestAirBubbleQuality != null) {
            return bestAirBubbleQuality;
        } else {
            return Config.getAirQualityAtLevelByDimension(level.dimension().location(), (int) Math.round(location.y));
        }
    }

    public static boolean isSensitiveToAirQuality(LivingEntity entity) {
        return entity.getType().is(ModRegistry.AIR_QUALITY_SENSITIVE_ENTITY_TYPE_TAG)
                && (!(entity instanceof Player player) || !player.getAbilities().invulnerable);
    }
}
