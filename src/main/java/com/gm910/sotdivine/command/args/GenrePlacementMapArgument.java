package com.gm910.sotdivine.command.args;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.util.TextUtils;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;

public class GenrePlacementMapArgument implements ArgumentType<Map<String, IPlaceableGenreProvider<?, ?>>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("{\"X\":{\"blocks\":\"#minecraft:banners\"}}",
			"{\"B\":\"minecraft:lectern\"}");

	private static final DynamicCommandExceptionType ERROR_MAP_FORMAT = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.map_format.invalid", p_308347_));

	private static final DynamicCommandExceptionType ERROR_STRING_LENGTH = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.map_format.length", p_308347_));

	private static final CommandArgumentParser<JsonElement> TAG_PARSER = SnbtGrammar.createParser(JsonOps.INSTANCE);

	private CommandBuildContext access;

	private RegistryOps<JsonElement> ops;

	public GenrePlacementMapArgument(CommandBuildContext context) {
		access = context;
		ops = RegistryOps.create(JsonOps.INSTANCE, access);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static Map<String, IPlaceableGenreProvider<?, ?>> getArgument(CommandContext<CommandSourceStack> context,
			String arg) throws CommandSyntaxException {

		return context.getArgument(arg, Map.class);
	}

	@Override
	public Map<String, IPlaceableGenreProvider<?, ?>> parse(StringReader reader) throws CommandSyntaxException {
		JsonElement element = TAG_PARSER.parseForCommands(reader);
		Codec<IPlaceableGenreProvider<?, ?>> decoder = IGenreProvider.castCodec(IPlaceableGenreProvider.class);

		var result = Codec.unboundedMap(Codec.string(1, 1), decoder).decode(ops, element);
		if (result.isError()) {
			throw ERROR_MAP_FORMAT.create(result);
		}
		LogUtils.getLogger().debug("Map: {}", result);
		return result.result().get().getFirst();
	}

}
