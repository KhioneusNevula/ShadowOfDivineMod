package com.gm910.sotdivine.util;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

/**
 * Utils for accessing fields
 */
public class FieldUtils {

	private FieldUtils() {
	}

	/**
	 * Gets a field given a name from the given object
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	public static <R> R getInstanceField(String name, Object object) {
		return FieldUtils.getInstanceField(name, name, object);
	}

	/**
	 * Gets a field given a name and obfuscated name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R getInstanceField(String name, String obfName, Object object) {
		return (R) FieldUtils.getField((Class) object.getClass(), name, obfName, object);
	}

	/**
	 * Gets a static field given a name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R getStaticField(Class<?> clazz, String name) {
		return (R) FieldUtils.getStaticField((Class) clazz, name, name);
	}

	/**
	 * Gets a static field given a name and obfuscated name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R getStaticField(Class<?> clazz, String name, String obfName) {
		return (R) FieldUtils.getField((Class) clazz, name, obfName, null);
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
	public static <T> void setInstanceField(String name, Object object, T value) {
		FieldUtils.setInstanceField(name, name, object, value);
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
	public static <T> void setInstanceField(String name, String obfName, Object object, T value) {
		FieldUtils.setField((Class) object.getClass(), name, obfName, object, value);
	}

	/**
	 * Reflectively sets a static field of a class
	 * 
	 * @param name
	 * @param obfName
	 * @param object
	 * @param value
	 */
	public static <T> void setStaticField(Class<?> clazz, String name, T value) {
		FieldUtils.setStaticField(clazz, name, name, value);
	}

	/**
	 * Reflectively sets a static field of a class
	 * 
	 * @param name
	 * @param obfName
	 * @param object
	 * @param value
	 */
	public static <T> void setStaticField(Class<?> clazz, String name, String obfName, T value) {
		FieldUtils.setField(clazz, name, obfName, null, value);
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
