package com.gm910.sotdivine.mixins_assist.gui;

import java.util.List;

import com.gm910.sotdivine.network.party_system.ClientParties;
import com.gm910.sotdivine.util.MethodUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;

public class LoomScreenUtils {
	private LoomScreenUtils() {
	}

	public static final ResourceLocation DIVINE_PATTERN_SPRITE = ModUtils.path("container/loom/pattern");

	public static Holder<BannerPattern> getPatternAt(LoomMenu menu, boolean displayPatterns, int startRow, int leftPos,
			int topPos, int x, int y) {

		var selectablePatterns = menu.getSelectablePatterns();
		if (displayPatterns) {
			int i = leftPos + 60;
			int j = topPos + 13;

			for (int k = 0; k < 4; k++) {
				for (int l = 0; l < 4; l++) {
					double d0 = x - (i + l * 14);
					double d1 = y - (j + k * 14);
					int i1 = k + startRow;
					int j1 = i1 * 4 + l;
					if (d0 >= 0.0 && d1 >= 0.0 && d0 < 14.0 && d1 < 14.0 && j1 >= 0 && j1 < selectablePatterns.size()) {
						return selectablePatterns.get(j1);
					}
				}
			}
		}
		return null;
	}

	public static void renderBannerOnButton(GuiGraphics graphics, LoomMenu menu, boolean displayPatterns, int startRow,
			int leftPos, int topPos, int x, int y, TextureAtlasSprite sprite) {

		var patternAt = getPatternAt(menu, displayPatterns, startRow, leftPos, topPos, x, y);
		boolean isDivine = patternAt != null
				&& ClientParties.instance().get().deitiesByPattern(patternAt.get()).findAny().isPresent();

		if (isDivine)
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DIVINE_PATTERN_SPRITE, x, y, 14, 14);

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 4, y + 2);
		float f = sprite.getU0();
		float f1 = f + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
		float f2 = sprite.getV1() - sprite.getV0();
		float f3 = sprite.getV0() + f2 / 64.0F;
		float f4 = f3 + f2 * 40.0F / 64.0F;
		graphics.fill(0, 0, 5, 10,
				isDivine ? DyeColor.BLACK.getTextureDiffuseColor() : DyeColor.GRAY.getTextureDiffuseColor());
		/*if (isDivine) {
			MethodUtils.callInstanceMethod("innerBlit", "a", graphics,
					new Class<?>[] { RenderPipeline.class, ResourceLocation.class, int.class, int.class, int.class,
							int.class, float.class, float.class, float.class, float.class, int.class },
					RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), 0, 0, 5, 10, f, f1, f3, f4,
					DyeColor.YELLOW.getTextureDiffuseColor());
		
		} else {*/
		graphics.blit(sprite.atlasLocation(), 0, 0, 5, 10, f, f1, f3, f4);
		// }
		graphics.pose().popMatrix();
	}

	public static void renderDeityTooltip(GuiGraphics graphics, Font font, LoomMenu menu, boolean displayPatterns,
			int startRow, int leftPos, int topPos, int mouseX, int mouseY) {

		var hoveredPattern = LoomScreenUtils.getPatternAt(menu, displayPatterns, startRow, leftPos, topPos, mouseX,
				mouseY);
		if (hoveredPattern != null) {
			List<Component> list = Lists.newArrayList();
			ClientParties.instance().get().deitiesByPattern(hoveredPattern.get()).forEachOrdered(
					(dei) -> list.add(dei.descriptiveName().orElse(Component.literal(dei.uniqueName()))));
			graphics.setComponentTooltipForNextFrame(font, list, mouseX, mouseY);
		}
	}

}
