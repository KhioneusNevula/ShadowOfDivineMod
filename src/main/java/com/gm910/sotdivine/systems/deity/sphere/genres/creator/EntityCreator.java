package com.gm910.sotdivine.systems.deity.sphere.genres.creator;

import java.util.function.Consumer;

import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;

public class EntityCreator<T extends Entity> implements IGenreCreator {

	private EntityType<T> entityType;
	private Consumer<T> postCreate;

	public EntityCreator(EntityType<T> entity, Consumer<T> postCreation) {
		entityType = entity;
		this.postCreate = postCreation;

	}

	@Override
	public boolean tryPlace(ServerLevel level, BlockPos at) {
		Entity construct = this.createWithoutPlacing(level, at);
		if (construct != null)
			level.addFreshEntityWithPassengers(construct);
		return construct != null;
	}

	/**
	 * Create this entity without placing it
	 * 
	 * @param level
	 * @param at
	 * @return
	 */
	public Entity createWithoutPlacing(ServerLevel level, BlockPos at) {
		return entityType.create(level, postCreate, at, EntitySpawnReason.NATURAL, true, false);
	}

	@Override
	public ItemStack getAsItem(ServerLevel level, BlockPos pos) {
		boolean custom = false;
		Item egg = SpawnEggItem.byId(entityType);
		if (egg == null) {
			egg = Items.SKELETON_SPAWN_EGG;
			custom = true;
		}
		ItemStack stack = new ItemStack(egg);
		Entity e = createWithoutPlacing(level, pos);
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(e.serializeNBT(level.registryAccess())));
		if (custom) {
			stack.set(DataComponents.CUSTOM_NAME, TextUtils.transPrefix("item.custom.spawn_egg", e.getName()));
		}
		return stack;
	}

}
