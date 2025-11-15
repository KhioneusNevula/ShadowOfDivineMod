package com.gm910.sotdivine.concepts.genres.provider.entity;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.util.HolderUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Generates an effect emanation with duration 5 seconds; change it when you
 * receive it
 * 
 * @param level
 * @param emanation
 * @return
 */
public record MobEffectGenreProvider(HolderSet<MobEffect> effect, MinMaxBounds.Ints strength, Optional<Boolean> ambient,
		Optional<Boolean> visible) implements IEntityGenreProvider<MobEffectInstance, MobEffectInstance> {

	private static Codec<MobEffectGenreProvider> CODEC = null;

	public static final Codec<MobEffectGenreProvider> codec() {
		if (CODEC == null) {

			Codec<MobEffectGenreProvider> registryCodec = RegistryCodecs.homogeneousList(Registries.MOB_EFFECT)
					.flatComapMap((s) -> new MobEffectGenreProvider(s, MinMaxBounds.Ints.ANY, Optional.empty(),
							Optional.empty()), (s) -> {
								if (s.strength.equals(MinMaxBounds.Ints.ANY) && s.ambient.isEmpty()
										&& s.visible.isEmpty()) {
									return DataResult.success(s.effect);
								}
								return DataResult.error(
										() -> "No point in converting from complex effect provider into effect",
										s.effect);
							});

			Codec<MobEffectGenreProvider> construction = RecordCodecBuilder.create(instance -> instance
					.group(RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects")
							.forGetter(MobEffectGenreProvider::effect),
							MinMaxBounds.Ints.CODEC.optionalFieldOf("amplifier", MinMaxBounds.Ints.ANY)
									.forGetter(MobEffectGenreProvider::strength),
							Codec.BOOL.optionalFieldOf("ambient").forGetter(MobEffectGenreProvider::ambient),
							Codec.BOOL.optionalFieldOf("visible").forGetter(MobEffectGenreProvider::visible))
					.apply(instance, (e, s, a, v) -> new MobEffectGenreProvider(e, s, a, v)));
			CODEC = Codec.either(registryCodec, construction).xmap(Either::unwrap, Either::right);
		}
		return CODEC;
	}

	@Override
	public ProviderType<MobEffectGenreProvider> providerType() {
		return ProviderType.MOB_EFFECT_TYPE;
	}

	@Override
	public boolean matches(ServerLevel level, MobEffectInstance instance) {

		return HolderUtils.holderSetContains(effect, instance.getEffect()) && strength.matches(instance.getAmplifier())
				&& (ambient.isEmpty() ? true : ambient.get().equals(instance.isAmbient()))
				&& (visible.isEmpty() ? true : visible.get().equals(instance.isVisible()));
	}

	@Override
	public MobEffectInstance generateRandom(ServerLevel level, Optional<MobEffectInstance> prev) {
		Holder<MobEffect> eff = effect.getRandomElement(level.random).orElseThrow();
		int amplifier = level.random.nextIntBetweenInclusive(
				strength.min().orElse(prev.map(MobEffectInstance::getAmplifier).orElse(1)),
				strength.max().orElse(prev.map(MobEffectInstance::getAmplifier).orElse(1)));
		boolean ambient = this.ambient.orElse(prev.map(MobEffectInstance::isAmbient).orElse(false));
		boolean visible = this.visible.orElse(prev.map(MobEffectInstance::isVisible).orElse(true));
		return new MobEffectInstance(eff, prev.map((x) -> x.getDuration()).orElse(200), amplifier, ambient, visible);
	}

	@Override
	public boolean matchesEntity(ServerLevel level, Entity entity) {
		return entity instanceof LivingEntity en
				? en.getActiveEffects().stream().anyMatch((x) -> this.matches(level, x))
				: false;
	}

	@Override
	public Entity generateRandomForEntity(ServerLevel level, Optional<Entity> entityO) {
		Entity en = entityO.orElseThrow();
		if (en instanceof LivingEntity entity) {
			MobEffectInstance effect = this.generateRandom(level, Optional.empty());
			entity.addEffect(effect);
			return entity;
		}
		return en;
	}

	@Override
	public final String toString() {
		return "Effect(" + effect + "){" + TextUtils.printMinMaxBounds(strength) + "}";
	}

	@Override
	public Component translate() {
		Optional<Component> minNum = strength.min()
				.map((min) -> Component.translatable("enchantment.level." + (min + 1)));
		Optional<Component> maxNum = strength.max()
				.map((max) -> Component.translatable("enchantment.level." + (max + 1)));
		Component strengthRange = TextUtils.transPrefix(
				"sotd.genre.provider.effect.strength" + (minNum.isPresent() ? ".lower" : "")
						+ (maxNum.isPresent() ? ".upper" : ""),
				minNum.orElse(Component.literal(TextUtils.INFINITY + "")),
				maxNum.orElse(Component.literal(TextUtils.INFINITY + "")));

		return TextUtils.transPrefix("sotd.genre.provider.effect",
				effect.stream().filter(Holder::isBound).map(Holder::get).map((s) -> s.getDisplayName()), strengthRange,
				ambient.map((a) -> TextUtils.transPrefix("sotd.genre.provider.effect.ambient" + (a ? "" : ".not")))
						.orElse(Component.empty()),
				visible.map((a) -> TextUtils.transPrefix("sotd.genre.provider.effect.visible" + (a ? "" : ".not")))
						.orElse(Component.empty()));
	}

}
