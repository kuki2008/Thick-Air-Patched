package com.leclowndu93150.thick_air;

import com.leclowndu93150.thick_air.advancement.BreatheAirTrigger;
import com.leclowndu93150.thick_air.advancement.SignalifyTorchTrigger;
import com.leclowndu93150.thick_air.advancement.UsedSoulfireTrigger;
import com.leclowndu93150.thick_air.api.AirQualityLevel;
import com.leclowndu93150.thick_air.block.SafetyLanternBlock;
import com.leclowndu93150.thick_air.block.SignalTorchBlock;
import com.leclowndu93150.thick_air.block.WallSignalTorchBlock;
import com.leclowndu93150.thick_air.capability.AirBubblePositions;
import com.leclowndu93150.thick_air.item.AirBladderItem;
import com.leclowndu93150.thick_air.item.SoulfireBottleItem;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThickAir.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThickAir.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThickAir.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThickAir.MODID);

    public static final Holder<ArmorMaterial> RESPIRATOR_ARMOR_MATERIAL = registerArmorMaterial();

    public static final DeferredBlock<SignalTorchBlock> SIGNAL_TORCH_BLOCK = BLOCKS.register("signal_torch",
            () -> new SignalTorchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH)));

    public static final DeferredBlock<WallSignalTorchBlock> WALL_SIGNAL_TORCH_BLOCK = BLOCKS.register("wall_signal_torch",
            () -> new WallSignalTorchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH)
                    .lootFrom(SIGNAL_TORCH_BLOCK)));

    public static final DeferredBlock<SafetyLanternBlock> SAFETY_LANTERN_BLOCK = BLOCKS.register("safety_lantern",
            () -> new SafetyLanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                    .lightLevel(state -> state.getValue(SafetyLanternBlock.AIR_QUALITY).getLightLevel())));

    public static final DeferredItem<ArmorItem> RESPIRATOR_ITEM = ITEMS.register("respirator",
            () -> new ArmorItem(RESPIRATOR_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(77)));

    public static final DeferredItem<AirBladderItem> AIR_BLADDER_ITEM = ITEMS.register("air_bladder",
            () -> new AirBladderItem(new Item.Properties().durability(327)));

    public static final DeferredItem<AirBladderItem> REINFORCED_AIR_BLADDER_ITEM = ITEMS.register("reinforced_air_bladder",
            () -> new AirBladderItem(new Item.Properties().durability(1962)));

    public static final DeferredItem<SoulfireBottleItem> SOULFIRE_BOTTLE_ITEM = ITEMS.register("soulfire_bottle",
            () -> new SoulfireBottleItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> SAFETY_LANTERN_ITEM = ITEMS.register("safety_lantern",
            () -> new BlockItem(SAFETY_LANTERN_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<StandingAndWallBlockItem> SIGNAL_TORCH_ITEM = ITEMS.register("signal_torch",
            () -> new StandingAndWallBlockItem(SIGNAL_TORCH_BLOCK.get(), WALL_SIGNAL_TORCH_BLOCK.get(),
                    new Item.Properties(), net.minecraft.core.Direction.DOWN));

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ThickAir.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, BreatheAirTrigger> BREATHE_AIR_TRIGGER =
            TRIGGERS.register("breathe_air", BreatheAirTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, SignalifyTorchTrigger> SIGNALIFY_TORCH_TRIGGER =
            TRIGGERS.register("signalify_torch", SignalifyTorchTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, UsedSoulfireTrigger> USED_SOULFIRE_TRIGGER =
            TRIGGERS.register("used_soulfire", UsedSoulfireTrigger::new);

    public static final Supplier<AttachmentType<AirBubblePositions>> AIR_BUBBLE_POSITIONS =
            ATTACHMENT_TYPES.register("air_bubble_positions",
                    () -> AttachmentType.builder(AirBubblePositions::new)
                            .serialize(AirBubblePositions.CODEC)
                            .copyOnDeath()
                            .build());

    public static final TagKey<Item> AIR_REFILLER_ITEM_TAG = TagKey.create(Registries.ITEM, ThickAir.id("air_refiller"));
    public static final TagKey<EntityType<?>> AIR_QUALITY_SENSITIVE_ENTITY_TYPE_TAG = TagKey.create(Registries.ENTITY_TYPE, ThickAir.id("air_quality_sensitive"));

    public static final ResourceKey<LootTable> SOULFIRE_BOTTLE_BURIED_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/soulfire_bottle_buried"));
    public static final ResourceKey<LootTable> SOULFIRE_BOTTLE_SHIPWRECK_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/soulfire_bottle_shipwreck"));
    public static final ResourceKey<LootTable> SOULFIRE_BOTTLE_BIG_RUIN_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/soulfire_bottle_big_ruin"));
    public static final ResourceKey<LootTable> SOULFIRE_BOTTLE_SMALL_RUIN_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/soulfire_bottle_small_ruin"));
    public static final ResourceKey<LootTable> SAFETY_LANTERN_DUNGEON_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/safety_lantern_dungeon"));
    public static final ResourceKey<LootTable> SAFETY_LANTERN_MINESHAFT_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/safety_lantern_mineshaft"));
    public static final ResourceKey<LootTable> SAFETY_LANTERN_STRONGHOLD_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, ThickAir.id("chest/inject/safety_lantern_stronghold"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THICK_AIR_TAB = CREATIVE_MODE_TABS.register("thick_air",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thick_air"))
                    .icon(() -> new ItemStack(AIR_BLADDER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RESPIRATOR_ITEM.get());
                        output.accept(AIR_BLADDER_ITEM.get());
                        output.accept(REINFORCED_AIR_BLADDER_ITEM.get());
                        output.accept(SOULFIRE_BOTTLE_ITEM.get());
                        output.accept(SAFETY_LANTERN_ITEM.get());
                        output.accept(SIGNAL_TORCH_ITEM.get());
                    })
                    .build());

    private static Holder<ArmorMaterial> registerArmorMaterial() {
        return Holder.direct(new ArmorMaterial(
                java.util.Map.of(
                        ArmorItem.Type.HELMET, 1,
                        ArmorItem.Type.CHESTPLATE, 0,
                        ArmorItem.Type.LEGGINGS, 0,
                        ArmorItem.Type.BOOTS, 0,
                        ArmorItem.Type.BODY, 0
                ),
                15, // enchantability
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(Items.CHARCOAL),
                List.of(new ArmorMaterial.Layer(ThickAir.id("respirator"))),
                0.0F, // toughness
                0.0F  // knockback resistance
        ));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        TRIGGERS.register(modEventBus);
    }
}
