package com.gm910.sotdivine.util;

import java.util.HashSet;
import java.util.Set;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Some utilities for mod registration and mod cycles
 * 
 * @author borah
 *
 */
public class ModUtils {

	private ModUtils() {
	}

	/**
	 * Forcibly registers an item, unfreezing a registry in order to do it. If I
	 * could shatter these stupid registries to pieces I WOULD.
	 * 
	 * @param <T>
	 * @param registry
	 * @param location
	 * @param value
	 */
	public static <T> void forceRegister(IForgeRegistry<T> registry, ResourceLocation location, T value) {

		ForgeRegistry<T> stupidRegistry = (ForgeRegistry<T>) registry;
		boolean frozenBefore = FieldUtils.getInstanceField("isFrozen", stupidRegistry);

		FieldUtils.setInstanceField("isFrozen", stupidRegistry, false);

		if (registry.containsKey(location)) {
			LogUtils.getLogger().debug("Re-registering " + location + " to frozen registry with value " + value);
			stupidRegistry.remove(location);
		} else {
			LogUtils.getLogger().debug("Initially registering " + location + " to frozen registry with value " + value);

		}
		stupidRegistry.register(location, value);

		FieldUtils.setInstanceField("isFrozen", stupidRegistry, frozenBefore);
	}

	/**
	 * Return a resource location with the mod id
	 * 
	 * @param pathType
	 * @return
	 */
	public static ResourceLocation path(String path) {
		return ResourceLocation.fromNamespaceAndPath(SOTDMod.MODID, path);
	}

	/**
	 * If this resourcelocation is "minecraft:X" then shorten it to just X
	 * 
	 * @param loc
	 * @return
	 */
	public static String toShortString(ResourceLocation loc) {
		if (loc.getNamespace().equals(loc.DEFAULT_NAMESPACE)) {
			return loc.getPath();
		}
		return loc.toString();
	}

	/**
	 * returns all instances of a specific command in this command stack
	 * 
	 * @param parse
	 */
	public static Set<CommandContextBuilder<CommandSourceStack>> getInstancesOfCommand(
			CommandContextBuilder<CommandSourceStack> root, String command) {
		return getInstancesOfCommand(root, command, Set.of());
	}

	/**
	 * returns all instances of a specific command in this command stack
	 * 
	 * @param parse
	 */
	public static Set<CommandContext<CommandSourceStack>> getInstancesOfCommand(CommandContext<CommandSourceStack> root,
			String command) {
		return getInstancesOfCommand(root, command, Set.of());
	}

	private static Set<CommandContextBuilder<CommandSourceStack>> getInstancesOfCommand(
			CommandContextBuilder<CommandSourceStack> ctxt, String command,
			Set<CommandContextBuilder<CommandSourceStack>> contexts) {
		Set<CommandContextBuilder<CommandSourceStack>> set2 = new HashSet<>(contexts);
		if (ctxt.getNodes().stream().anyMatch((pn) -> pn.getNode().getName().equals(command))) {
			set2.add(ctxt);
		}
		if (ctxt.getChild() != null) {
			return getInstancesOfCommand(ctxt.getChild(), command, set2);
		}
		return set2;
	}

	private static Set<CommandContext<CommandSourceStack>> getInstancesOfCommand(
			CommandContext<CommandSourceStack> ctxt, String command, Set<CommandContext<CommandSourceStack>> contexts) {
		Set<CommandContext<CommandSourceStack>> set2 = new HashSet<>(contexts);
		if (ctxt.getRootNode().getName().equalsIgnoreCase(command)) {
			set2.add(ctxt);
		}
		if (ctxt.getChild() != null) {
			return getInstancesOfCommand(ctxt.getChild(), command, set2);
		}
		return set2;
	}

}
