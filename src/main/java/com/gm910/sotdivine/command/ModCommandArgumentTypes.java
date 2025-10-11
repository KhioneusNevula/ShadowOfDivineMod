package com.gm910.sotdivine.command;

import java.util.function.Supplier;

import com.gm910.sotdivine.SOTDMod;
import com.mojang.brigadier.arguments.ArgumentType;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.registries.RegistryObject;

/**
 * Argument types for commands for this mod
 * 
 * @author borah
 *
 */
public class ModCommandArgumentTypes {

	private ModCommandArgumentTypes() {
	}

	public static void init() {
		System.out.println("Initializing mod argument types...");
	}

	/**
	 * PArty identifier type
	 */
	public static final RegistryObject<ArgumentTypeInfo<PartyIdentifierArgument, SingletonArgumentInfo<PartyIdentifierArgument>.Template>> PARTY_IDENTIFIER = register(
			"party_identifier", PartyIdentifierArgument.class,
			() -> SingletonArgumentInfo.contextFree(PartyIdentifierArgument::argument));

	public static <T extends ArgumentType<?>, X extends ArgumentTypeInfo.Template<T>> RegistryObject<ArgumentTypeInfo<T, X>> register(
			String path, Class<T> clazz, Supplier<ArgumentTypeInfo<T, X>> supplier) {
		return SOTDMod.COMMAND_ARGUMENT_TYPES.register(path, () -> {
			ArgumentTypeInfo<T, X> info = supplier.get();
			ArgumentTypeInfos.registerByClass(clazz, info);
			return info;

		});
	}

}
