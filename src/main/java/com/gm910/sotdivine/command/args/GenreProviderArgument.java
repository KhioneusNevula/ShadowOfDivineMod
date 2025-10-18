package com.gm910.sotdivine.command.args;

import java.util.Arrays;
import java.util.Collection;

import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;
import com.gm910.sotdivine.util.TextUtils;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;

public class GenreProviderArgument<T> implements ArgumentType<T> {
	private static final Collection<String> EXAMPLES = Arrays.asList("{\"blocks\":\"#minecraft:banners\"}",
			"\"minecraft:lectern\"");

	private static final DynamicCommandExceptionType ERROR_PROVIDER_FORMAT = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.genre_format.invalid", p_308347_));
	private static final DynamicCommandExceptionType ERROR_GENRE_FORMAT = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.genre.invalid", p_308347_));
	public static final DynamicCommandExceptionType ERROR_GENRE_UNGIVEABLE = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.genre.nongiveable", p_308347_));
	public static final DynamicCommandExceptionType ERROR_GENRE_UNPLACEABLE = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("argument.genre.nonplaceable", p_308347_));

	private static final CommandArgumentParser<JsonElement> TAG_PARSER = SnbtGrammar.createParser(JsonOps.INSTANCE);

	private CommandBuildContext access;

	private RegistryOps<JsonElement> ops;

	public GenreProviderArgument(CommandBuildContext context) {
		access = context;
		ops = RegistryOps.create(JsonOps.INSTANCE, access);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static <T> T getArgument(CommandContext<CommandSourceStack> context, String arg, Class<? super T> clazz,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {

		T object = (T) context.getArgument(arg, clazz);
		if (!clazz.isInstance(object)) {
			throw exception.create(object);
		}
		return object;
	}

	@Override
	public T parse(StringReader reader) throws CommandSyntaxException {

		ResourceLocation genreLoc = ResourceLocation.read(reader);

		// ResourceKey<IGenreType<?>> lookupKey =
		// ResourceKey.create(ModRegistries.GENRE_TYPES, genreLoc);

		// RegistryLookup<IGenreType<?>> genres =
		// access.lookupOrThrow(ModRegistries.GENRE_TYPES);

		// Optional<Reference<IGenreType<?>>> genreHolder = genres.get(lookupKey);

		IGenreType<?> genre = GenreTypes.getGenreType(genreLoc);

		if (genre == null) {
			throw ERROR_GENRE_FORMAT.create(genreLoc);
		}

		JsonElement element = TAG_PARSER.parseForCommands(reader);

		var result = genre.classCodec().decode(ops, element);
		if (result.isError()) {
			throw ERROR_PROVIDER_FORMAT.create(result);
		}
		LogUtils.getLogger().debug("Provider: {}", result);
		T res = (T) result.result().get().getFirst();
		return res;
	}

}
