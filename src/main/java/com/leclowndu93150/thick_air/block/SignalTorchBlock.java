package com.leclowndu93150.thick_air.block;

import com.leclowndu93150.thick_air.Config;
import com.leclowndu93150.thick_air.ModRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class SignalTorchBlock extends TorchBlock {
    public static final MapCodec<SignalTorchBlock> CODEC = simpleCodec(SignalTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SignalTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            // Signal torch spawns firework particles
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
            // Additional firework-like particles
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FIREWORK, x, y + 0.5, z,
                        random.nextGaussian() * 0.05, random.nextDouble() * 0.05, random.nextGaussian() * 0.05);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!Config.enableSignalTorches) {
            return InteractionResult.PASS;
        }

        boolean newLit = !state.getValue(LIT);
        level.setBlockAndUpdate(pos, state.setValue(LIT, newLit));
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (player instanceof ServerPlayer serverPlayer) {
            ModRegistry.SIGNALIFY_TORCH_TRIGGER.get().trigger(serverPlayer);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Static event handler for converting normal torches to signal torches
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableSignalTorches) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // Only convert empty-handed or with non-torch items
        if (!stack.isEmpty()) return;

        if (state.is(Blocks.TORCH)) {
            level.setBlockAndUpdate(pos, ModRegistry.SIGNAL_TORCH_BLOCK.get().defaultBlockState());
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player instanceof ServerPlayer serverPlayer) {
                ModRegistry.SIGNALIFY_TORCH_TRIGGER.get().trigger(serverPlayer);
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        } else if (state.is(Blocks.WALL_TORCH)) {
            BlockState signalState = ModRegistry.WALL_SIGNAL_TORCH_BLOCK.get().defaultBlockState()
                    .setValue(WallSignalTorchBlock.FACING, state.getValue(net.minecraft.world.level.block.WallTorchBlock.FACING));
            level.setBlockAndUpdate(pos, signalState);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player instanceof ServerPlayer serverPlayer) {
                ModRegistry.SIGNALIFY_TORCH_TRIGGER.get().trigger(serverPlayer);
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }
}
