package com.gm910.sotdivine.common.command.args;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.other.MagicWord;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class MagicWordArgument implements ArgumentType<Collection<MagicWord>> {

	private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("cmd.magic_word.invalid", p_308347_));

	public static MagicWordArgument argument() {
		return new MagicWordArgument();
	}

	public static Collection<MagicWord> getWords(CommandContext<CommandSourceStack> stack, String name) {
		return stack.getArgument(name, Collection.class);
	}

	@Override
	public Collection<MagicWord> parse(StringReader reader) throws CommandSyntaxException {
		try {
			String quoted = reader.readQuotedString();
			List<MagicWord> vals = Spheres.instance().getSphereMap().values().stream()
					.flatMap((s) -> s.getGenres(GenreTypes.MAGIC_WORD).stream())
					.filter((mw) -> mw.translation().getString().matches(quoted)).distinct().toList();
			if (vals.isEmpty()) {
				throw ERROR_INVALID_PATTERN.create(quoted);
			}
			return vals;
		} catch (CommandSyntaxException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new DynamicCommandExceptionType(p_308347_ -> Component.literal(e.getMessage())).create(null);
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {

		Spheres.instance().getSphereMap().values().stream().flatMap((s) -> s.getGenres(GenreTypes.MAGIC_WORD).stream())
				.distinct().forEach((pattern) -> builder.suggest("\"" + pattern.translation().getString() + "\""));
		return builder.buildFuture();

	}

	@Override
	public Collection<String> getExamples() {
		return Set.of("\"Yamaraja\"");
	}
}
