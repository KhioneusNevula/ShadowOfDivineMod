package com.gm910.sotdivine.mixins.entity;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin extends HangingEntity implements ISymbolBearer {

	protected ItemFrameMixin(EntityType<? extends HangingEntity> p_31703_, Level p_31704_) {
		super(p_31703_, p_31704_);
	}

	public boolean convertAllSymbols(IDeitySymbol toSymbol) {
// TODO some 'category' finder for deity symbol items
		return false;
	}

	public Stream<? extends IDeitySymbol> getSymbols() {
		return getItem().getCapability(ISymbolBearer.CAPABILITY).lazyMap((s) -> s.getSymbols()).orElse(Stream.empty());
	}

	public boolean hasAnySymbol() {
		return getItem().getCapability(ISymbolBearer.CAPABILITY).lazyMap((s) -> s.hasAnySymbol()).orElse(false);
	}

	public boolean hasSymbol(IDeitySymbol sym) {
		return getItem().getCapability(ISymbolBearer.CAPABILITY).lazyMap((s) -> s.hasSymbol(sym)).orElse(false);
	}

	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	public abstract void setItem(ItemStack p_31790_);
}
