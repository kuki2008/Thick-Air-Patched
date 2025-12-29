package com.leclowndu93150.thick_air.capability;

import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AirBubblePositions {
    private static final Codec<AirQualityLevel> AIR_QUALITY_CODEC = Codec.STRING.xmap(
            s -> AirQualityLevel.valueOf(s.toUpperCase()),
            AirQualityLevel::getSerializedName
    );

    private record Entry(BlockPos pos, AirQualityLevel quality) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                AIR_QUALITY_CODEC.fieldOf("quality").forGetter(Entry::quality)
        ).apply(instance, Entry::new));
    }

    public static final Codec<AirBubblePositions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(Entry.CODEC).fieldOf("entries").forGetter(AirBubblePositions::toEntryList),
            Codec.INT.optionalFieldOf("skipCountLeft", 0).forGetter(cap -> cap.skipCountLeft)
    ).apply(instance, AirBubblePositions::fromEntryList));

    private int skipCountLeft;
    private Map<BlockPos, AirQualityLevel> airBubbleEntries = new LinkedHashMap<>();

    public AirBubblePositions() {
    }

    private static AirBubblePositions fromEntryList(List<Entry> entries, int skipCountLeft) {
        AirBubblePositions cap = new AirBubblePositions();
        cap.skipCountLeft = skipCountLeft;
        for (Entry entry : entries) {
            cap.airBubbleEntries.put(entry.pos, entry.quality);
        }
        return cap;
    }

    private List<Entry> toEntryList() {
        return airBubbleEntries.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .toList();
    }

    public Map<BlockPos, AirQualityLevel> getAirBubblePositionsView() {
        return Collections.unmodifiableMap(this.airBubbleEntries);
    }

    public Map<BlockPos, AirQualityLevel> getAirBubblePositions() {
        return this.airBubbleEntries;
    }

    public int getSkipCountLeft() {
        return this.skipCountLeft;
    }

    public void setSkipCountLeft(int skipCountLeft) {
        this.skipCountLeft = skipCountLeft;
    }
}
