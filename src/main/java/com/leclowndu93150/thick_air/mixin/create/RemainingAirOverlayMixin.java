package com.leclowndu93150.thick_air.mixin.create;

import com.leclowndu93150.thick_air.compat.CreateCompat;
import com.simibubi.create.content.equipment.armor.RemainingAirOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RemainingAirOverlay.class)
public class RemainingAirOverlayMixin {

    /**
     * Redirects the isAir check to also return false when in bad air quality,
     * so the backtank overlay shows up in YELLOW/RED air zones.
     */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/FluidType;isAir()Z")
    )
    private boolean redirectIsAir(FluidType fluidType) {
        if (CreateCompat.shouldActivateDivingHelmet(Minecraft.getInstance().player)) {
            return false;
        }
        return fluidType.isAir();
    }
}
