package com.leclowndu93150.thick_air;

import com.leclowndu93150.thick_air.block.SignalTorchBlock;
import com.leclowndu93150.thick_air.compat.CuriosCompat;
import com.leclowndu93150.thick_air.handler.AirBubbleTracker;
import com.leclowndu93150.thick_air.handler.DrownedAttackHandler;
import com.leclowndu93150.thick_air.handler.TickAirHandler;
import com.leclowndu93150.thick_air.network.ChunkAirQualityPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(ThickAir.MODID)
public class ThickAir {
    public static final String MODID = "thick_air";
    public static final String MOD_NAME = "Thick Air";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final TagKey<Item> AIR_REFILLER_ITEM_TAG = TagKey.create(Registries.ITEM, id("air_refiller"));

    public ThickAir(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModRegistry.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        CuriosCompat.registerCapabilities(modEventBus);

        NeoForge.EVENT_BUS.addListener(TickAirHandler::onLivingBreathe);
        NeoForge.EVENT_BUS.addListener(DrownedAttackHandler::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(AirBubbleTracker::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(AirBubbleTracker::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(AirBubbleTracker::onChunkWatch);
        NeoForge.EVENT_BUS.addListener(AirBubbleTracker::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(AirBubbleTracker::onLevelTick);
        NeoForge.EVENT_BUS.addListener(SignalTorchBlock::onBlockInteract);
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                ChunkAirQualityPayload.TYPE,
                ChunkAirQualityPayload.STREAM_CODEC,
                ChunkAirQualityPayload::handleClient
        );
    }

    private void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();

        // Inject soulfire bottles into underwater loot
        if (id.equals(BuiltInLootTables.BURIED_TREASURE)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SOULFIRE_BOTTLE_BURIED_LOOT_TABLE));
        } else if (id.equals(BuiltInLootTables.SHIPWRECK_TREASURE)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SOULFIRE_BOTTLE_SHIPWRECK_LOOT_TABLE));
        } else if (id.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SOULFIRE_BOTTLE_BIG_RUIN_LOOT_TABLE));
        } else if (id.equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SOULFIRE_BOTTLE_SMALL_RUIN_LOOT_TABLE));
        }
        // Inject safety lanterns into dungeon loot
        else if (id.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SAFETY_LANTERN_DUNGEON_LOOT_TABLE));
        } else if (id.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SAFETY_LANTERN_MINESHAFT_LOOT_TABLE));
        } else if (id.equals(BuiltInLootTables.STRONGHOLD_CORRIDOR)) {
            event.getTable().addPool(createInjectionPool(ModRegistry.SAFETY_LANTERN_STRONGHOLD_LOOT_TABLE));
        }
    }

    private static LootPool createInjectionPool(ResourceKey<LootTable> lootTable) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(NestedLootTable.lootTableReference(lootTable))
                .build();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
