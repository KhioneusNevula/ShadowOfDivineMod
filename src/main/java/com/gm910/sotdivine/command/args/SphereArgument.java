package com.gm910.sotdivine.command.args;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.ISphere;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.Spheres;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

/**
 * Argument of a party's unique identifying name
 * 
 * @author borah
 *
 */
public class SphereArgument implements ArgumentType<ISphere> {

	private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType(
			p_308347_ -> TextUtils.transPrefix("cmd.sphere.invalid", p_308347_));

	public static SphereArgument argument() {
		return new SphereArgument();
	}

	public static ISphere getSphere(CommandContext<CommandSourceStack> stack, String name)
			throws CommandSyntaxException {
		return stack.getArgument(name, ISphere.class);
	}

	@Override
	public ISphere parse(StringReader reader) throws CommandSyntaxException {
		ResourceLocation loc = ResourceLocation.read(reader);
		ISphere pattern = Spheres.instance().sphere(loc);
		if (pattern == null) {
			throw ERROR_INVALID_PATTERN.create(loc);
		}
		return pattern;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {

		Spheres.instance().getSphereMap().keySet().stream().forEach((pattern) -> builder.suggest(pattern.toString()));
		return builder.buildFuture();

	}

	@Override
	public Collection<String> getExamples() {
		return Set.of("<namespace>:<path>");
	}

}
