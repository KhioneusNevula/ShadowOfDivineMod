package com.gm910.sotdivine.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

/**
 * Utils for accessing fields
 */
public class MethodUtils {

	private MethodUtils() {
	}

	/**
	 * Gets an instance method with a name but no need for obfuscation name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	public static <R> R callInstanceMethod(String name, Object object, Class<?>[] argTypes, Object... args) {
		return callInstanceMethod(name, name, object, argTypes, args);
	}

	/**
	 * Gets an instance method with a name and obfuscation name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R callInstanceMethod(String name, String obfName, Object object, Class<?>[] argTypes,
			Object... args) {
		return (R) callMethod((Class) object.getClass(), name, obfName, object, argTypes, args);
	}

	/**
	 * Gets a static method without needing obfuscation name
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "rawtypes" })
	public static <R> R callStaticMethod(Class<?> clazz, String name, Class<?>[] argTypes, Object... args) {
		return callStaticMethod((Class) clazz, name, name, argTypes, args);
	}

	/**
	 * Gets a static method and calls it
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <R> R callStaticMethod(Class<?> clazz, String name, String obfName, Class<?>[] argTypes,
			Object... args) {
		return (R) callMethod((Class) clazz, name, obfName, null, argTypes, args);
	}

	/**
	 * Runs a method of a class; if object is null get static method
	 * 
	 * @param clazz
	 * @param name
	 * @param obfName
	 */
	@SuppressWarnings("unchecked")
	public static <T, R> R callMethod(Class<? super T> clazz, String name, String obfName, @Nullable T object,
			Class<?>[] argTypes, Object... args) {
		Method meth;
		try {
			meth = clazz.getDeclaredMethod(name, argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			try {
				meth = clazz.getDeclaredMethod(obfName, argTypes);
			} catch (NoSuchMethodException | SecurityException e1) {
				throw new RuntimeException(e1);
			}
		}
		meth.setAccessible(true);
		try {
			return (R) meth.invoke(object, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
