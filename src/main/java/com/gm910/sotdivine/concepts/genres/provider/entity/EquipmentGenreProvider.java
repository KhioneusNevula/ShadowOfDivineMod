package com.gm910.sotdivine.concepts.genres.provider.entity;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.ProviderType;
import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.util.CodecUtils;
import com.gm910.sotdivine.util.StreamUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

public record EquipmentGenreProvider(Map<EquipmentSlot, ItemGenreProvider> map)
		implements IEntityGenreProvider<Entity, Entity> {

	public EquipmentGenreProvider(Map<EquipmentSlot, ItemGenreProvider> map) {
		this.map = Map.copyOf(map);
	}

	private static Codec<EquipmentGenreProvider> CODEC;

	public static Codec<EquipmentGenreProvider> codec() {
		if (CODEC == null)
			CODEC = Codec.unboundedMap(CodecUtils.caselessEnumCodec(EquipmentSlot.class), ItemGenreProvider.codec())
					.xmap(EquipmentGenreProvider::new, EquipmentGenreProvider::map);
		return CODEC;
	}

	@Override
	public ProviderType<EquipmentGenreProvider> providerType() {
		return ProviderType.EQUIPMENT;
	}

	@Override
	public boolean matches(ServerLevel level, Entity instance) {
		if (instance instanceof LivingEntity entity) {
			for (EquipmentSlot slot : map.keySet()) {
				if (entity.getItemBySlot(slot) instanceof ItemStack stack) {
					if (!map.get(slot).matches(level, stack))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Entity generateRandom(ServerLevel level, Optional<Entity> entity) {
		LivingEntity livingEntity = entity.filter((x) -> x instanceof LivingEntity).map((x) -> (LivingEntity) x)
				.orElse(null);
		if (livingEntity == null) {
			livingEntity = new ArmorStand(EntityType.ARMOR_STAND, level);
		}
		for (EquipmentSlot slot : this.map.keySet()) {
			livingEntity.setItemSlot(slot,
					map.get(slot).generateRandom(level, Optional.ofNullable(livingEntity.getItemBySlot(slot)))
							.getAsItem(level, BlockPos.ZERO));
		}
		return livingEntity;
	}

	@Override
	public boolean matchesEntity(ServerLevel level, Entity entity) {
		return this.matches(level, entity);
	}

	@Override
	public Entity generateRandomForEntity(ServerLevel level, Optional<Entity> entity) {
		return this.generateRandom(level, entity);
	}

	@Override
	public final String toString() {
		return "Equipment" + this.map;
	}

	@Override
	public Component translate() {
		return this.map.entrySet().stream()
				.map((s) -> Map.entry(TextUtils.transPrefix("sotd.cmd.slot." + s.getKey().getName()),
						TextUtils.transPrefix("sotd.cmd.parenthesis", s.getValue().translate())))
				.collect(StreamUtils.componentCollector(TextUtils.PRETTY_LIST_TRANSLATION_PREFIX, null,
						(x) -> TextUtils.transPrefix("sotd.cmd.map.colon", x.getKey(), x.getValue())));
	}

}
