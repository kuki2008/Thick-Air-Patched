package com.leclowndu93150.thick_air.item;

import com.leclowndu93150.thick_air.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class SoulfireBottleItem extends Item {

    public SoulfireBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Restore full air supply
            player.setAirSupply(player.getMaxAirSupply());

            // Play sounds
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

            // Spawn soul particles
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 10; i++) {
                    double offsetX = level.random.nextGaussian() * 0.5;
                    double offsetY = level.random.nextDouble() * 1.0;
                    double offsetZ = level.random.nextGaussian() * 0.5;
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                            player.getX() + offsetX,
                            player.getY() + offsetY + 0.5,
                            player.getZ() + offsetZ,
                            1, 0, 0.1, 0, 0.05);
                }
            }

            // Trigger advancement
            if (player instanceof ServerPlayer serverPlayer) {
                ModRegistry.USED_SOULFIRE_TRIGGER.get().trigger(serverPlayer);
            }

            // Statistics
            player.awardStat(Stats.ITEM_USED.get(this));

            // Consume item
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
