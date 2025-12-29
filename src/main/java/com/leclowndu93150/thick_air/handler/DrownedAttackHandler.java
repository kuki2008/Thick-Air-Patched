package com.leclowndu93150.thick_air.handler;

import com.leclowndu93150.thick_air.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class DrownedAttackHandler {

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (Config.drownedChoking <= 0) return;

        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof Drowned) {
            int newAir = Math.max(0, target.getAirSupply() - Config.drownedChoking);
            target.setAirSupply(newAir);
        }
    }
}
