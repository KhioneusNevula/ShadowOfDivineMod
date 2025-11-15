package com.gm910.sotdivine.mixins.block;

import org.spongepowered.asm.mixin.Mixin;

import com.gm910.sotdivine.mixins_assist.block.BlockMixinHelper;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.extensions.IForgeBlockState;

@Mixin(BlockState.class)
public abstract class BlockStateMixin extends BlockStateBase implements IForgeBlockState {

	protected BlockStateMixin(Block p_60608_, Reference2ObjectArrayMap<Property<?>, Comparable<?>> p_332547_,
			MapCodec<BlockState> p_60610_) {
		super(p_60608_, p_332547_, p_60610_);
	}

	@Override
	public void onPlace(Level level, BlockPos pos, BlockState oldState, boolean p_60700_) {
		BlockMixinHelper.onPlace(level, pos, oldState, p_60700_);
	}

	@Override
	public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState) {
		BlockMixinHelper.onStateChange(level, pos, oldState);
	}

}
