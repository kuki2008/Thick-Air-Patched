package com.leclowndu93150.thick_air.compat;

import com.leclowndu93150.thick_air.api.AirQualityHelper;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import net.minecraft.world.entity.LivingEntity;

public class CreateCompat {
    /**
     * Check if the current air quality should activate Create's diving helmet.
     * Returns true for YELLOW and RED air quality (bad air).
     */
    public static boolean shouldActivateDivingHelmet(LivingEntity entity) {
        AirQualityLevel airQuality = AirQualityHelper.getAirQualityAtLocation(entity);
        return airQuality == AirQualityLevel.YELLOW || airQuality == AirQualityLevel.RED;
    }
}
