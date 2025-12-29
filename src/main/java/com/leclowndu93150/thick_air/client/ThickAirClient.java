package com.leclowndu93150.thick_air.client;

import com.leclowndu93150.thick_air.ModRegistry;
import com.leclowndu93150.thick_air.ThickAir;
import com.leclowndu93150.thick_air.api.AirQualityHelper;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.leclowndu93150.thick_air.block.SafetyLanternBlock;
import com.leclowndu93150.thick_air.compat.CuriosClientCompat;
import com.leclowndu93150.thick_air.compat.CuriosCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ThickAir.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ThickAirClient {
    public static final ResourceLocation AIR_QUALITY_LEVEL_PROPERTY = ThickAir.id("air_quality_level");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModRegistry.SAFETY_LANTERN_ITEM.get(), AIR_QUALITY_LEVEL_PROPERTY,
                    (ItemStack stack, ClientLevel level, LivingEntity entity, int seed) -> {
                        if (stack.has(DataComponents.BLOCK_STATE)) {
                            var blockState = stack.get(DataComponents.BLOCK_STATE);
                            if (blockState != null) {
                                var props = blockState.properties();
                                for (var entry : props.entrySet()) {
                                    if (entry.getKey().equals(SafetyLanternBlock.AIR_QUALITY.getName())) {
                                        String value = entry.getValue();
                                        try {
                                            AirQualityLevel airQuality = AirQualityLevel.valueOf(value.toUpperCase());
                                            return airQuality.getItemModelProperty();
                                        } catch (IllegalArgumentException ignored) {}
                                    }
                                }
                            }
                        }

                        if (entity == null && stack.getEntityRepresentation() instanceof LivingEntity livingEntity) {
                            entity = livingEntity;
                        }

                        AirQualityLevel airQualityAtLocation;
                        if (entity != null) {
                            airQualityAtLocation = AirQualityHelper.getAirQualityAtLocation(entity);
                        } else {
                            airQualityAtLocation = AirQualityLevel.YELLOW;
                        }
                        return airQualityAtLocation.getItemModelProperty();
                    });

            if (CuriosCompat.isCuriosLoaded()) {
                CuriosClientCompat.registerCuriosRenderer();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RespiratorRenderer.PLAYER_RESPIRATOR_LAYER, RespiratorRenderer::createLayerDefinition);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        RespiratorRenderer.bakeModel(Minecraft.getInstance().getEntityModels());

        for (PlayerSkin.Model skin : event.getSkins()) {
            LivingEntityRenderer<Player, PlayerModel<Player>> renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new RespiratorArmorLayer<>(renderer));
            }
        }
    }
}
