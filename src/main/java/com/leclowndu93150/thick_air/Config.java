package com.leclowndu93150.thick_air;

import com.leclowndu93150.thick_air.api.AirQualityLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@EventBusSubscriber(modid = ThickAir.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS = BUILDER
            .comment("Air qualities at different heights in different dimensions.",
                    "The syntax is the dimension's resource location, the \"default\" air level in that dimension,",
                    "then any number of height:airlevel pairs separated by commas.",
                    "The air will have that quality starting at that height and above (until the next entry).",
                    "The entries must be in ascending order of height.",
                    "If a dimension doesn't have an entry here, it'll be assumed to be green everywhere.")
            .defineList("dimensions",
                    Arrays.asList(
                            "minecraft:overworld=yellow,0:green,128:yellow",
                            "minecraft:the_nether=yellow",
                            "minecraft:the_end=red"
                    ),
                    o -> o instanceof String s && parseDimensionLine(s) != null
            );

    private static final ModConfigSpec.BooleanValue ENABLE_SIGNAL_TORCHES = BUILDER
            .comment("Whether to allow right-clicking torches to make them spray particle effects")
            .define("enableSignalTorches", true);

    private static final ModConfigSpec.IntValue DROWNED_CHOKING = BUILDER
            .comment("How much air a Drowned attack removes. Set to 0 to disable this feature.")
            .defineInRange("drownedChoking", 100, 0, 72000);

    private static final ModConfigSpec.DoubleValue YELLOW_AIR_PROVIDER_RADIUS = BUILDER
            .push("Ranges")
            .comment("The radius in which all blocks defined in the yellow air providers tag project a bubble of air around them.")
            .defineInRange("yellowAirProviderRadius", 6.0, 1.0, 32.0);

    private static final ModConfigSpec.DoubleValue BLUE_AIR_PROVIDER_RADIUS = BUILDER
            .comment("The radius in which all blocks defined in the blue air providers tag (usually soul fire related blocks) project a bubble of air around them.")
            .defineInRange("blueAirProviderRadius", 6.0, 1.0, 32.0);

    private static final ModConfigSpec.DoubleValue RED_AIR_PROVIDER_RADIUS = BUILDER
            .comment("The radius in which all blocks defined in the red air providers tag (usually lava related blocks) project a bubble of air around them.")
            .defineInRange("redAirProviderRadius", 3.0, 1.0, 32.0);

    private static final ModConfigSpec.DoubleValue GREEN_AIR_PROVIDER_RADIUS = BUILDER
            .comment("The radius in which all blocks defined in the green air providers tag (usually various portal blocks) project a bubble of air around them.")
            .defineInRange("greenAirProviderRadius", 9.0, 1.0, 32.0);

    static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // Cached values
    public static boolean enableSignalTorches;
    public static int drownedChoking;
    public static double yellowAirProviderRadius;
    public static double blueAirProviderRadius;
    public static double redAirProviderRadius;
    public static double greenAirProviderRadius;
    private static Map<ResourceLocation, DimensionEntry> dimensionEntries = null;

    @Nullable
    private static Pair<ResourceLocation, DimensionEntry> parseDimensionLine(String line) {
        var dimensionVals = line.split("=");
        if (dimensionVals.length != 2) {
            ThickAir.LOGGER.warn("Couldn't parse dimension line {}: couldn't split across `=` into 2 parts", line);
            return null;
        }

        var dimkey = ResourceLocation.tryParse(dimensionVals[0]);
        if (dimkey == null) {
            ThickAir.LOGGER.warn("Couldn't parse dimension line {}: {} isn't a valid resource location",
                    line, dimensionVals[0]);
            return null;
        }

        var heightAndRest = dimensionVals[1].split(",", 2);
        AirQualityLevel baseQuality;
        try {
            baseQuality = AirQualityLevel.valueOf(heightAndRest[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ThickAir.LOGGER.warn("Couldn't parse dimension line {}: {} isn't a valid base air quality",
                    line, heightAndRest[0]);
            return null;
        }

        var heights = new ArrayList<Pair<Integer, AirQualityLevel>>();
        if (heightAndRest.length == 2) {
            var heightPairStrs = heightAndRest[1].split(",");
            Integer prevHeight = null;
            for (var heightPairStr : heightPairStrs) {
                var pairStr = heightPairStr.split(":");
                if (pairStr.length != 2) {
                    ThickAir.LOGGER.warn("Couldn't parse dimension line {}: couldn't use {} as a height entry",
                            line, heightPairStr);
                    return null;
                }

                int height;
                try {
                    height = Integer.parseInt(pairStr[0]);
                } catch (NumberFormatException e) {
                    ThickAir.LOGGER.warn("Couldn't parse dimension line {}: {} isn't a valid int", line, pairStr[0]);
                    return null;
                }
                if (prevHeight != null && height <= prevHeight) {
                    return null;
                }
                prevHeight = height;

                AirQualityLevel quality;
                try {
                    quality = AirQualityLevel.valueOf(pairStr[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    ThickAir.LOGGER.warn("Couldn't parse dimension line {}: {} isn't a valid air quality",
                            line, pairStr[1]);
                    return null;
                }

                heights.add(Pair.of(height, quality));
            }
        }

        return Pair.of(dimkey, new DimensionEntry(baseQuality, heights));
    }

    public static AirQualityLevel getAirQualityAtLevelByDimension(ResourceLocation dimension, int y) {
        if (dimensionEntries == null) {
            // Parse the dimension configuration
            var lines = DIMENSIONS.get();
            dimensionEntries = new HashMap<>(lines.size());
            for (var line : lines) {
                var entry = parseDimensionLine(line);
                if (entry == null) {
                    ThickAir.LOGGER.warn("Somehow managed to get a bad dimension config past the validator?!");
                    continue;
                }
                dimensionEntries.put(entry.getLeft(), entry.getRight());
            }
        }

        if (dimensionEntries.containsKey(dimension)) {
            var entry = dimensionEntries.get(dimension);
            List<Pair<Integer, AirQualityLevel>> heights = entry.heights;
            for (int i = 0; i < heights.size(); i++) {
                Pair<Integer, AirQualityLevel> heightPair = heights.get(heights.size() - i - 1);
                if (y >= heightPair.getLeft()) {
                    return heightPair.getRight();
                }
            }
            return entry.baseQuality;
        } else {
            return AirQualityLevel.GREEN;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableSignalTorches = ENABLE_SIGNAL_TORCHES.get();
        drownedChoking = DROWNED_CHOKING.get();
        yellowAirProviderRadius = YELLOW_AIR_PROVIDER_RADIUS.get();
        blueAirProviderRadius = BLUE_AIR_PROVIDER_RADIUS.get();
        redAirProviderRadius = RED_AIR_PROVIDER_RADIUS.get();
        greenAirProviderRadius = GREEN_AIR_PROVIDER_RADIUS.get();
        // Reset dimension entries cache so it gets reparsed
        dimensionEntries = null;
    }

    private record DimensionEntry(AirQualityLevel baseQuality, List<Pair<Integer, AirQualityLevel>> heights) {}
}
