package com.leclowndu93150.thick_air.mixin.create;

import com.leclowndu93150.thick_air.compat.CreateCompat;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DivingHelmetItem.class)
public class DivingHelmetItemMixin {

    /**
     * Redirects the canBreathe check to also return false when in bad air quality,
     * so the diving helmet activates in YELLOW/RED air zones.
     */
    @Redirect(
            method = "breatheUnderwater",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/entity/living/LivingBreatheEvent;canBreathe()Z")
    )
    private static boolean redirectCanBreathe(LivingBreatheEvent event) {
        if (CreateCompat.shouldActivateDivingHelmet(event.getEntity())) {
            return false;
        }
        return event.canBreathe();
    }
}
