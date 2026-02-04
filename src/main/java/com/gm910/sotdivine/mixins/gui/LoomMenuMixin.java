package com.gm910.sotdivine.mixins.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gm910.sotdivine.magic.impression.IImpression;
import com.gm910.sotdivine.magic.impression.cap.IMindsEye;
import com.gm910.sotdivine.magic.impression.client.ImpressionsClient;
import com.gm910.sotdivine.magic.impression.types.DeityImpression;
import com.google.common.collect.Streams;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;

@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu {

	@Final
	@Shadow
	private HolderGetter<BannerPattern> patternGetter;

	private Player player;

	protected LoomMenuMixin(MenuType<?> p_38851_, int p_38852_) {
		super(p_38851_, p_38852_);
	}

	@Inject(method = "<init>*", at = @At(value = "RETURN"), require = 1)
	public void LoomMenu(int cid, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.player = inventory.player;
	}

	@Inject(method = "getSelectablePatterns", at = @At("HEAD"), require = 1, cancellable = true)
	public void getSelectablePatterns(ItemStack stack, CallbackInfoReturnable<List<Holder<BannerPattern>>> ci) {

		if (stack.isEmpty()) {
			List<Holder<BannerPattern>> holders = new ArrayList<>();
			Consumer<Stream<IImpression>> checkForPatterns = stream -> {
				stream.flatMap((i) -> i instanceof DeityImpression di ? Stream.of(di) : Stream.empty())
						.sorted((di, di2) -> di.deity().compareTo(di2.deity())).forEach((imp) -> {
							imp.getDeityInfo(player.level()).ifPresent(deity -> {
								holders.add(this.patternGetter
										.getOrThrow(deity.symbol().bannerPattern().unwrapKey().get()));
							});
						});
			};
			if (player.level().isClientSide) {
				ImpressionsClient.runOnAllImpressions(checkForPatterns);
			} else {
				checkForPatterns.accept(Streams.stream(IMindsEye.get(player).getAllImpressions()));
			}
			this.patternGetter.get(BannerPatternTags.NO_ITEM_REQUIRED).ifPresent((x) -> x.forEach(y -> holders.add(y)));
			ci.setReturnValue(holders);
		}
	}
}
