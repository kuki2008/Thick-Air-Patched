package com.leclowndu93150.thick_air.handler;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.api.AirQualityHelper;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

public class TickAirHandler {

    public static void onLivingBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();

        if (!AirQualityHelper.isSensitiveToAirQuality(entity)) return;

        AirQualityLevel airQualityLevel = AirQualityHelper.getAirQualityAtLocation(entity);

        if (entity instanceof ServerPlayer player) {
            ModRegistry.BREATHE_AIR_TRIGGER.get().trigger(player, airQualityLevel);
        }

        int airAmount = airQualityLevel.getAirAmountAfterProtection(entity);

        // Modify the event based on air quality
        // airAmount > 0 means gaining air (GREEN quality)
        // airAmount < 0 means losing air (YELLOW or RED quality without protection)
        // airAmount == 0 means no change (BLUE quality or protected)

        if (airAmount > 0) {
            // Gaining air (GREEN quality) - can breathe and refill
            event.setCanBreathe(true);
            event.setRefillAirAmount(airAmount);
        } else if (airAmount < 0) {
            // Losing air (YELLOW or RED quality without protection)
            // Must set canBreathe to false AND set consume amount
            event.setCanBreathe(false);
            event.setConsumeAirAmount(-airAmount);
            event.setRefillAirAmount(0);
        } else {
            // No change (BLUE quality or protected) - can breathe and slowly refill
            event.setCanBreathe(true);
            if (entity.level().getGameTime() % 4 == 0) {
                event.setRefillAirAmount(1);
            } else {
                event.setRefillAirAmount(0);
            }
        }
    }
}
