package com.gm910.sotdivine.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class WorldUtils {

	private WorldUtils() {

	}

	/**
	 * Removestag from entity's data
	 * 
	 * @param e
	 * @param pathType
	 */
	public static void deleteTag(Entity e, String path) {
		accessNestedTag(e, path, false).ifPresent(pair -> {
			pair.getSecond().remove(pair.getFirst());
		});
	}

	/**
	 * Return if this has a given tag
	 * 
	 * @param e
	 * @param tagga
	 * @return
	 */
	public static boolean hasTag(Entity e, String tagga) {
		return accessNestedTag(e, tagga, false).map((i) -> i.getSecond().get(i.getFirst())).isPresent();
	}

	/**
	 * returns a tag, paired with a final key that would access the int value within
	 * th tag that is layered into this using a pathType separated by dots, e.g.
	 * "sanctuary.max" points to "sanctuary": { "max":... }, which will be created
	 * as needed if "create" is true
	 * 
	 * @param divisibleName
	 * @return
	 */
	public static Optional<Pair<String, CompoundTag>> accessNestedTag(Entity e, String path, boolean create) {
		List<String> pathParts = Lists.newArrayList(path.split("."));
		CompoundTag topTag = e.get(DataComponents.CUSTOM_DATA).copyTag();
		if (pathParts.isEmpty()) {
			return Optional.of(Pair.of(path, topTag));
		}
		String itag = pathParts.removeLast();
		CompoundTag curTag = topTag;
		for (String tagName : pathParts) {
			final CompoundTag ctCopy = curTag;
			curTag = curTag.getCompound(tagName).orElseGet(() -> {
				if (!create)
					return null;
				CompoundTag tg = new CompoundTag();
				ctCopy.put(tagName, tg);
				return tg;
			});
			if (curTag == null)
				return Optional.empty();
		}
		if (create)
			e.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(topTag));
		return Optional.ofNullable(Pair.of(itag, curTag));
	}

	/**
	 * Return false if no such int tag exsts
	 * 
	 * @param e
	 * @param name
	 * @param by
	 * @return
	 */
	public static boolean changeIntTag(Entity e, String name, int by) {
		return getIntTag(e, name).map((inte) -> {
			setIntTag(e, name, inte + by);
			return inte;
		}).isPresent();
	}

	/**
	 * Returns an int tag from the entity
	 * 
	 * @param e
	 * @param name
	 * @return
	 */
	public static Optional<Integer> getIntTag(Entity e, String name) {
		return accessNestedTag(e, name, false).flatMap(f -> f.getSecond().getInt(f.getFirst()));
	}

	/**
	 * Adds an integer nbt tag to this entity's custom data
	 * 
	 * @param e
	 * @param name
	 * @param num
	 */
	public static void setIntTag(Entity e, String name, int num) {
		Pair<String, CompoundTag> tag = accessNestedTag(e, name, true)
				.orElseThrow(() -> new IllegalArgumentException("Some issue getting tag of " + name));
		tag.getSecond().putInt(tag.getFirst(), num);
	}

	/**
	 * Return the central block pos (with y = 0) of a group of chunkposes
	 * 
	 * @param others
	 * @return
	 */
	public static BlockPos centerCP(Collection<ChunkPos> others) {
		return BlockPos.containing(others.stream().map((c) -> c.getMiddleBlockPosition(0)).map(BlockPos::getCenter)
				.reduce(Vec3.ZERO, (a, b) -> a.add(b)).scale(1.0 / others.size()));
	}

	/**
	 * Gets the center/average of a group of blocks
	 * 
	 * @param others
	 * @return
	 */
	public static BlockPos center(Collection<BlockPos> others) {
		return BlockPos.containing(others.stream().map(BlockPos::getCenter).reduce(Vec3.ZERO, (a, b) -> a.add(b))
				.scale(1.0 / others.size()));
	}

}
