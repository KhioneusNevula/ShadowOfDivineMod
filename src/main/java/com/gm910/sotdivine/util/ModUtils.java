package com.gm910.sotdivine.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.annotation.Nullable;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Some utilities
 * 
 * @author borah
 *
 */
public class ModUtils {

	/**
	 * Simple uuid codec to/from string
	 */
	public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

	private ModUtils() {
	}

	public static <In> Collector<In, ?, String> setStringCollector(String delim) {
		return Collector.of(StringBuilder::new, (a, b) -> (a.isEmpty() ? a : a.append(delim)).append(b),
				(a, b) -> a.append(delim).append(b), StringBuilder::toString);
	}

	/**
	 * So we can easily obtain this and replace with translate later
	 * 
	 * @param string
	 * @return
	 */
	public static Component literal(String string) {
		return Component.literal(string);
	}

	/**
	 * Just for simplicity idk; prefixes "commands.sotd" to the beginning if it is
	 * not there already
	 * 
	 * @param string
	 * @return
	 */
	public static Component trans(String string, Object... list) {
		if (!string.startsWith("sotd.")) {
			string = "sotd." + string;
		}
		return Component.translatableEscape(string, list);
	}

	/**
	 * Return a resource location with the mod id
	 * 
	 * @param path
	 * @return
	 */
	public static ResourceLocation path(String path) {
		return ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, path);
	}

	/**
	 * Return the central block pos (with y = 0) of a group of chunkposes
	 * 
	 * @param others
	 * @return
	 */
	public static BlockPos centerC(Collection<ChunkPos> others) {
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

	/**
	 * Gets a field given a name and obfuscated name; if object is null get static
	 * field
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R getField(String name, String obfName, Object object) {
		return (R) getField((Class) object.getClass(), name, obfName, object);
	}

	/**
	 * Gets a field given a name and obfuscated name; if object is null get static
	 * field
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R getField(Class<?> clazz, String name, String obfName) {
		return (R) getField((Class) clazz, name, obfName, null);
	}

	/**
	 * Gets a field given a name and obfuscated name; if object is null get static
	 * field
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings("unchecked")
	public static <T, R> R getField(Class<? super T> clazz, String name, String obfName, @Nullable T object) {
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			try {
				field = clazz.getDeclaredField(obfName);
			} catch (NoSuchFieldException e1) {
				throw new RuntimeException(e1);
			}
		}
		field.setAccessible(true);
		try {
			return (R) field.get(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reflectively sets a field for a given object
	 * 
	 * @param name
	 * @param obfName
	 * @param object
	 * @param value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setField(String name, String obfName, Object object, Object value) {
		setField((Class) object.getClass(), name, obfName, object, value);
	}

	/**
	 * Reflectively sets a static field of a class
	 * 
	 * @param name
	 * @param obfName
	 * @param object
	 * @param value
	 */
	public static void setField(Class<?> clazz, String name, String obfName, Object value) {
		setField(clazz, name, obfName, null, value);
	}

	/**
	 * Sets a field given a name and obfuscated name; if object is null set static
	 * field
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	public static <T> void setField(Class<? super T> clazz, String name, String obfName, @Nullable T object,
			Object value) {
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			try {
				field = clazz.getDeclaredField(obfName);
			} catch (NoSuchFieldException e1) {
				throw new RuntimeException(e1);
			}
		}
		field.setAccessible(true);
		try {
			field.set(object, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
