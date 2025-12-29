package com.leclowndu93150.thick_air.mixin;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.client.RespiratorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public HumanoidArmorLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        if (slot == EquipmentSlot.HEAD) {
            ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.is(ModRegistry.RESPIRATOR_ITEM.get())) {
                RespiratorRenderer renderer = RespiratorRenderer.get();
                if (renderer != null) {
                    renderer.render(helmet, poseStack, this.getParentModel(), buffer, packedLight, RenderType::armorCutoutNoCull);
                }
                ci.cancel();
            }
        }
    }
}
