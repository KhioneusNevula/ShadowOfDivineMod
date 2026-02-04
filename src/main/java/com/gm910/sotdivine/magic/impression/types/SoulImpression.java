package com.gm910.sotdivine.magic.impression.types;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.Soul;
import com.gm910.sotdivine.magic.impression.IImpression;
import com.gm910.sotdivine.magic.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.impression.ImpressionType;
import com.gm910.sotdivine.magic.impression.MentalState;
import com.gm910.sotdivine.magic.impression.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.impression.client.ImpressionsClient;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;

/**
 * Impression of a deity's presence/etc
 */
public class SoulImpression implements IImpression {

	public static StreamCodec<RegistryFriendlyByteBuf, SoulImpression> streamCodec() {
		return StreamCodec.composite(Soul.STREAM_CODEC, (si) -> si.soul,
				ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ENTITY_TYPE)),
				(si) -> Optional.ofNullable(si.entityType), ComponentSerialization.OPTIONAL_STREAM_CODEC,
				(si) -> Optional.ofNullable(si.name), ItemStack.OPTIONAL_STREAM_CODEC, (si) -> si.fakeItem,
				SoulImpression::new);
	}

	public static Codec<SoulImpression> codec() {
		return RecordCodecBuilder
				.create(instance -> instance.group(Soul.CODEC.fieldOf("soul").forGetter(SoulImpression::soul))
						.apply(instance, SoulImpression::new));
	}

	private Soul soul;

	private Component name;
	private EntityType<?> entityType;

	private ItemStack fakeItem = ItemStack.EMPTY;

	public SoulImpression(Soul soul) {
		this(soul, Optional.empty(), Optional.empty(), ItemStack.EMPTY);

	}

	private SoulImpression(Soul soul, Optional<EntityType<?>> type, Optional<Component> name, ItemStack fakeItem) {
		this.soul = soul;
		this.entityType = type.orElse(null);
		this.name = name.orElse(null);
		this.fakeItem = fakeItem;
	}

	/**
	 * Item stack used as a display item
	 * 
	 * @return
	 */
	public ItemStack getDisplayItem() {
		return fakeItem;
	}

	/**
	 * Return the display name of the represented entity
	 * 
	 * @return
	 */
	public Component getDisplayName() {
		return Optional.ofNullable(name).orElse(Component.translatable("sotd.unknown").withStyle(ChatFormatting.RED));
	}

	/**
	 * Return the type of the represented entity
	 * 
	 * @return
	 */
	public EntityType<?> getEntityType() {
		return Optional.<EntityType<?>>ofNullable(entityType).orElse(EntityType.PLAYER);
	}

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
		return ImpressionType.SOUL.get();
	}

	/**
	 * The deity this Impression is from
	 * 
	 * @return
	 */
	public Soul soul() {
		return soul;
	}

	@Override
	public Shape calculateShape(GuiGraphics graphics, int index, int numberOfNodes, DeltaTracker tracker,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState) {
		int length = 24;
		int height = 20;
		if (state == MentalState.AWAKE) {
			if (!fakeItem.isEmpty()) {

			} else {
				var cout = this.getPrintOutput(null, List.of());
				// length = Minecraft.getInstance().font.width(cout);

				height += 5 + Minecraft.getInstance().font.wordWrapHeight(cout, length);
			}
		}

		return new Ellipse2D.Double(0, 0, Math.min(75, length), height);
	}

	@Override
	public void render(GuiGraphics graphics, int idx, int outOf, DeltaTracker tracker, Optional<Shape> shape,
			MentalState state, ImpressionTimetracker instance, CompoundTag storedState) {
		shape.ifPresent(sha -> {
			Rectangle2D actualRect = sha.getBounds2D();

			boolean highVis = state == MentalState.MEDITATING;
			boolean clicked = ImpressionsClient.clickedShape(sha);
			float hue = (float) (Math
					.sin(Minecraft.getInstance().level.getGameTime() * 0.005 + soul.hashCode() + 0 * idx) + 1) * 0.5f;
			float sat = ((Mth
					.cos(Minecraft.getInstance().level.getGameTime() * 0.05f + soul.hashCode() * soul.hashCode())
					+ (highVis ? 6f : 3f)) / 7f);
			float bri = ((Mth
					.cos(Minecraft.getInstance().level.getGameTime() * 0.05f + soul.hashCode() * soul.hashCode())
					+ (highVis ? 6f : 3f)) / 7f);
			Color c1 = Color.getHSBColor(hue, sat, bri);
			float[] colores = c1.getRGBComponents(null);

			switch (state) {
			case AWAKE:
				float alpha = ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.1f + soul.hashCode() + idx)
						+ 1f) / 2f) * 0.3f;

				ImpressionsClient.blit(graphics, RenderPipelines.GUI_TEXTURED,
						ResourceLocation.withDefaultNamespace("textures/particle/bubble.png"), (int) actualRect.getX(),
						(int) actualRect.getY(), 0, 0, (int) actualRect.getWidth(), (int) actualRect.getHeight(),
						(int) actualRect.getWidth(), (int) actualRect.getHeight(),
						new Color(colores[0], colores[1], colores[2], alpha).getRGB());
				break;

			case ASLEEP:
			case MEDITATING:
				alpha = ((Mth.cos(Minecraft.getInstance().level.getGameTime() * 0.1f + soul.hashCode() + idx)
						+ (clicked ? 9f : (highVis ? 5f : 1f))) / 10f);

				graphics.drawWordWrap(Minecraft.getInstance().font, this.getPrintOutput(null, List.of()),
						(int) actualRect.getX(), (int) actualRect.getY() + (fakeItem.isEmpty() ? 0 : 24),
						(int) actualRect.getWidth(), Color.white.getRGB(), true);
				graphics.renderItem(Minecraft.getInstance().player, fakeItem, (int) actualRect.getX(),
						(int) actualRect.getY(), 0);

				// ResourceLocation path =
				// DeitySymbols.instance().getSymbolIconPath(deity.symbol());
				// graphics.blitSprite(RenderPipelines.GUI_TEXTURED, path, (int)
				// actualRect.getX(),
				// (int) actualRect.getY(), (int) actualRect.getWidth(), (int)
				// actualRect.getHeight(),
				// new Color(colores[0], colores[1], colores[2], alpha).getRGB());

				break;

			case WITNESSING:

				break;
			}

		});
	}

	@Override
	public void runServerTick(ServerLevel level, LivingEntity thinker, MentalState mentalState,
			ImpressionTimetracker timeInfo) {
		if (level.getGameTime() % 20 == 0) {
			this.soul.getExistingEntity(level).ifPresentOrElse((en) -> {
				this.entityType = en.getType();
				this.name = en.getName();
			}, () -> {
				this.soul.getSoulInfo(level).ifPresent(tag -> {
					this.entityType = tag.getEntityType();
					this.name = tag.getName(level);
				});
			});
		}
	}

	@Override
	public boolean shouldServerUpdateClientNow(ServerLevel level, LivingEntity thinker, MentalState state,
			ImpressionTimetracker instance) {
		if (level.getGameTime() % 20 == 0) {
			return true;
		}
		return false;

	}

	@Override
	public void onReceiveUpdateFromServer(MentalState state, ImpressionTimetracker instance) {
		if (this.entityType == EntityType.PLAYER) {
			this.fakeItem = new ItemStack(Items.PLAYER_HEAD);
			this.fakeItem.applyComponentsAndValidate(DataComponentPatch.builder()
					.set(DataComponents.PROFILE,
							new ResolvableProfile(Optional.empty(), Optional.of(this.soul.uuid()), new PropertyMap()))
					.build());
		} else {
			this.fakeItem = ItemStack.EMPTY;
		}
	}

	@Override
	public void showInformation(GuiGraphics graphics, int index, Rectangle maxRect, MentalState state,
			ImpressionTimetracker instance) {
		// TODO Auto-generated method stub

	}

	@Override
	public int hashCode() {
		return soul.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof SoulImpression di)
			return this.soul.equals(di.soul());
		return false;
	}

	@Override
	public String toString() {
		return "Imp_s((" + soul + "))";
	}

	@Override
	public Component getPrintOutput(ServerLevel levelRef, List<ImpressionHolder> outs) {
		return this.getDisplayName();
	}

}
