package com.gm910.sotdivine.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gm910.sotdivine.mixins_assist.gui.LoomScreenUtils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
	@Shadow
	private boolean displayPatterns;
	@Shadow
	private int startRow;

	protected LoomScreenMixin(LoomMenu p_97741_, Inventory p_97742_, Component p_97743_) {
		super(p_97741_, p_97742_, p_97743_);
	}

	@Overwrite
	private void renderBannerOnButton(GuiGraphics graphics, int x, int y, TextureAtlasSprite sprite) {
		LoomScreenUtils.renderBannerOnButton(graphics, menu, displayPatterns, startRow, leftPos, topPos, x, y, sprite);
	}

	@Inject(method = "render", at = @At("RETURN"), require = 1)
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float p_281886_, CallbackInfo ci) {
		LoomScreenUtils.renderDeityTooltip(graphics, font, menu, displayPatterns, startRow, leftPos, topPos, mouseX,
				mouseY);
	}

}
