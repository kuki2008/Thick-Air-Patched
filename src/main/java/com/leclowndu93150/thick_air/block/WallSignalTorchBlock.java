package com.leclowndu93150.thick_air.block;

import com.leclowndu93150.thick_air.Config;
import com.leclowndu93150.thick_air.ModRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class WallSignalTorchBlock extends WallTorchBlock {
    private static final MapCodec<WallSignalTorchBlock> CODEC = simpleCodec(WallSignalTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public WallSignalTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, true));
    }

    @Override
    public MapCodec<WallTorchBlock> codec() {
        return (MapCodec<WallTorchBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            Direction direction = state.getValue(FACING);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            Direction opposite = direction.getOpposite();
            x += 0.27 * opposite.getStepX();
            z += 0.27 * opposite.getStepZ();

            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
            // Additional firework-like particles
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FIREWORK, x, y + 0.3, z,
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
}
