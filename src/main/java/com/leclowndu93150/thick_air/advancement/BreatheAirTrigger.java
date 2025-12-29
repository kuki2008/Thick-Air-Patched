package com.leclowndu93150.thick_air.advancement;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class BreatheAirTrigger extends SimpleCriterionTrigger<BreatheAirTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, AirQualityLevel airQuality) {
        this.trigger(player, instance -> instance.matches(airQuality));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, AirQualityLevel airQuality)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                AirQualityLevel.CODEC.fieldOf("air_quality").forGetter(TriggerInstance::airQuality)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(AirQualityLevel airQuality) {
            return this.airQuality == airQuality;
        }

        public static Criterion<TriggerInstance> breatheAir(AirQualityLevel airQuality) {
            return new Criterion<>(
                    ModRegistry.BREATHE_AIR_TRIGGER.get(),
                    new TriggerInstance(Optional.empty(), airQuality)
            );
        }
    }
}
