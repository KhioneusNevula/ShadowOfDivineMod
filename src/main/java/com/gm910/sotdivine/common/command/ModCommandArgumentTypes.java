package com.gm910.sotdivine.common.command;

import java.util.function.Supplier;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.command.args.GenrePlacementMapArgument;
import com.gm910.sotdivine.common.command.args.GenreProviderArgument;
import com.gm910.sotdivine.common.command.args.MagicWordArgument;
import com.gm910.sotdivine.common.command.args.RitualPatternArgument;
import com.gm910.sotdivine.common.command.args.SphereArgument;
import com.gm910.sotdivine.common.command.args.SymbolArgument;
import com.gm910.sotdivine.common.command.args.party.DeityArgument;
import com.gm910.sotdivine.common.command.args.party.PartyArgument;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.logging.LogUtils;

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
		LogUtils.getLogger().debug("Initializing mod argument types...");
	}

	/**
	 * Magic word argument type
	 */
	public static final RegistryObject<ArgumentTypeInfo<MagicWordArgument, SingletonArgumentInfo<MagicWordArgument>.Template>> MAGIC_WORD = register(
			"magic_word", MagicWordArgument.class,
			() -> SingletonArgumentInfo.contextFree(MagicWordArgument::argument));

	/**
	 * PArty argument type
	 */
	public static final RegistryObject<ArgumentTypeInfo<PartyArgument, SingletonArgumentInfo<PartyArgument>.Template>> PARTY = register(
			"party", PartyArgument.class, () -> SingletonArgumentInfo.contextFree(PartyArgument::argument));

	/**
	 * Deity argument type
	 */
	public static final RegistryObject<ArgumentTypeInfo<DeityArgument, SingletonArgumentInfo<DeityArgument>.Template>> DEITY = register(
			"deity", DeityArgument.class, () -> SingletonArgumentInfo.contextFree(DeityArgument::argument));

	/**
	 * GenreType map type
	 */
	public static final RegistryObject<ArgumentTypeInfo<GenrePlacementMapArgument, SingletonArgumentInfo<GenrePlacementMapArgument>.Template>> GENRE_PREDICATE_MAP = register(
			"genre_predicate_map", GenrePlacementMapArgument.class,
			() -> SingletonArgumentInfo.contextAware(GenrePlacementMapArgument::new));

	/**
	 * GenreType provider type
	 */
	public static final RegistryObject<ArgumentTypeInfo<GenreProviderArgument, SingletonArgumentInfo<GenreProviderArgument>.Template>> GENRE_PROVIDER = register(
			"genre_provider", GenreProviderArgument.class,
			() -> SingletonArgumentInfo.contextAware((s) -> new GenreProviderArgument(s)));

	public static final RegistryObject<ArgumentTypeInfo<RitualPatternArgument, SingletonArgumentInfo<RitualPatternArgument>.Template>> RITUAL_PATTERN = register(
			"ritual_pattern", RitualPatternArgument.class,
			() -> SingletonArgumentInfo.contextFree(RitualPatternArgument::argument));

	public static final RegistryObject<ArgumentTypeInfo<SphereArgument, SingletonArgumentInfo<SphereArgument>.Template>> SPHERE = register(
			"sphere", SphereArgument.class, () -> SingletonArgumentInfo.contextFree(SphereArgument::argument));

	public static final RegistryObject<ArgumentTypeInfo<SymbolArgument, SingletonArgumentInfo<SymbolArgument>.Template>> SYMBOL = register(
			"symbol", SymbolArgument.class, () -> SingletonArgumentInfo.contextFree(SymbolArgument::argument));

	public static <T extends ArgumentType<?>, X extends ArgumentTypeInfo.Template<T>> RegistryObject<ArgumentTypeInfo<T, X>> register(
			String path, Class<T> clazz, Supplier<ArgumentTypeInfo<T, X>> supplier) {
		return SOTDMod.COMMAND_ARGUMENT_TYPES.register(path, () -> {
			ArgumentTypeInfo<T, X> info = supplier.get();
			ArgumentTypeInfos.registerByClass(clazz, info);
			return info;

		});
	}

}
