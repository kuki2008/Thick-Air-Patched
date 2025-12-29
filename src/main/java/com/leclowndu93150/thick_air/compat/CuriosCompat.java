package com.leclowndu93150.thick_air.compat;

import com.leclowndu93150.thick_air.ModRegistry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Optional;

public class CuriosCompat {
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    public static ItemStack findEquippedItem(LivingEntity entity, TagKey<Item> tag) {
        if (!CURIOS_LOADED) {
            return ItemStack.EMPTY;
        }
        return findEquippedItemCurios(entity, tag);
    }

    private static ItemStack findEquippedItemCurios(LivingEntity entity, TagKey<Item> tag) {
        Optional<SlotResult> result = CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.is(tag)));
        return result.map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static boolean isCuriosLoaded() {
        return CURIOS_LOADED;
    }

    public static void registerCapabilities(IEventBus modEventBus) {
        if (!CURIOS_LOADED) return;

        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }

                @Override
                public boolean canEquipFromUse(SlotContext slotContext) {
                    return true;
                }
            }, ModRegistry.RESPIRATOR_ITEM.get());
        });
    }
}
