package com.gm910.sotdivine.magic.theophany.impression.types;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.concepts.parties.IPartyLister;
import com.gm910.sotdivine.concepts.parties.IPartyLister.IDeityInfo;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.client.ImpressionsClient;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.magic.theophany.impression.MentalState;
import com.gm910.sotdivine.network.party_system.ClientParties;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Impression of a deity's presence/etc
 */
public class DeityImpression implements IImpression {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

	public static Codec<DeityImpression> codec() {
		return RecordCodecBuilder
				.create(instance -> instance.group(Codec.STRING.fieldOf("deity").forGetter(DeityImpression::deity))
						.apply(instance, DeityImpression::new));
	}

	public DeityImpression(String deity) {
		this.deity = deity;
	}

	private String deity;

	@Override
	public void activate(ServerLevel level, LivingEntity activator, List<ImpressionHolder> inputs,
			ImpressionTimetracker instance) {
		activator.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
	}

	@Override
	public List<ImpressionType<?>> requireInputs() {
		return List.of();
	}

	@Override
	public ImpressionType<?> getImpressionType() {
		return ImpressionType.DEITY.get();
	}

	/**
	 * The deity this Impression is from
	 * 
	 * @return
	 */
	public String deity() {
		return deity;
	}

	public Optional<IDeityInfo> getDeityInfo(Level world) {
		if (world.isClientSide) {
			return ClientParties.instance().flatMap((c) -> c.getDeityByName(this.deity));
		}
		return IPartySystem.get((ServerLevel) world).getDeityByName(this.deity).map(s -> (IDeityInfo) s);
	}

	@Override
	public Shape calculateShape(GuiGraphics graphics, int index, int numberOfNodes, DeltaTracker tracker,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState) {
		float length = 24;
		if (state == MentalState.AWAKE) {
			length = 24 * ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.05f + index * index) + 1f) / 2f);
		}

		return new Ellipse2D.Double(0, 0, Math.min(graphics.guiWidth() / (float) numberOfNodes, length),
				Math.min(graphics.guiHeight() / (float) numberOfNodes, length));
	}

	@Override
	public void render(GuiGraphics graphics, int idx, int outOf, DeltaTracker tracker, Optional<Shape> shape,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState) {
		shape.ifPresent(sha -> {
			Rectangle2D actualRect = sha.getBounds2D();
			Optional<? extends IDeityInfo> oDeity = IPartyLister.getLister(null).getDeityByName(this.deity);

			boolean highVis = state == MentalState.MEDITATING;
			boolean clicked = ImpressionsClient.clickedShape(sha);

			oDeity.ifPresent((deity) -> {
				// int width = minecraft.font.width("[" + toString() + "]");
				// int height = minecraft.font.lineHeight;

				float hue = (float) (Math
						.sin(Minecraft.getInstance().level.getGameTime() * 0.005 + this.deity.hashCode() + 0 * idx) + 1)
						* 0.5f;
				float sat = ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.05f
						+ this.deity.hashCode() * this.deity.hashCode()) + (highVis ? 6f : 3f)) / 7f);
				float bri = ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.05f
						+ this.deity.hashCode() * this.deity.hashCode()) + (highVis ? 6f : 3f)) / 7f);
				Color c1 = Color.getHSBColor(hue, sat, bri);
				float[] colores = c1.getRGBComponents(null);

				switch (state) {
				case AWAKE:
					float alpha = ((Mth
							.cos(Minecraft.getInstance().level.getGameTime() * 0.1f + this.deity.hashCode() + idx) + 1f)
							/ 2f) * 0.3f;
					int alphabetta_idx = Math.min(
							(int) (((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.001f + idx) + 1f) / 2f)
									* (ALPHABET.length() - 1)),
							ALPHABET.length() - 1);

					ImpressionsClient.blit(graphics, RenderPipelines.GUI_TEXTURED,
							ResourceLocation.withDefaultNamespace(
									"textures/particle/sga_" + ALPHABET.charAt(alphabetta_idx) + ".png"),
							(int) actualRect.getX(), (int) actualRect.getY(), 0, 0, (int) actualRect.getWidth(),
							(int) actualRect.getHeight(), (int) actualRect.getWidth(), (int) actualRect.getHeight(),
							new Color(colores[0], colores[1], colores[2], alpha).getRGB());
					break;

				case ASLEEP:
				case MEDITATING:
					alpha = ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.1f + this.deity.hashCode() + idx)
							+ (clicked ? 9f : (highVis ? 5f : 1f))) / 10f);
					ResourceLocation path = DeitySymbols.instance().getSymbolIconPath(deity.symbol());
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, path, (int) actualRect.getX(),
							(int) actualRect.getY(), (int) actualRect.getWidth(), (int) actualRect.getHeight(),
							new Color(colores[0], colores[1], colores[2], alpha).getRGB());

					break;

				case WITNESSING:

					break;
				}
			});
		});
	}

	@Override
	public void showInformation(GuiGraphics graphics, int index, Rectangle maxRect, MentalState state,
			ImpressionTimetracker instance) {
		// TODO Auto-generated method stub

	}

	@Override
	public int hashCode() {
		return deity.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof DeityImpression di)
			return this.deity.equals(di.deity());
		return false;
	}

	@Override
	public String toString() {
		return "Imp_d((" + deity + "))";
	}

	@Override
	public Component getPrintOutput(ServerLevel levelRef, List<ImpressionHolder> outs) {
		return (levelRef == null ? ClientParties.instance().get() : IPartySystem.get(levelRef)).getDeityByName(deity)
				.map((n) -> n.descriptiveName()
						.orElse(Component.literal(n.uniqueName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)))
				.orElse(Component.translatable("sotd.unknown").withStyle(ChatFormatting.RED));
	}

}
