package com.gm910.sotdivine.command;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.gm910.sotdivine.networking.PartySystemClient;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.util.ModUtils;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

/**
 * Argument of a party's unique identifying name
 * 
 * @author borah
 *
 */
public class PartyIdentifierArgument implements ArgumentType<String> {

	private PartyIdentifierArgument() {
	}

	public static PartyIdentifierArgument argument() {
		return new PartyIdentifierArgument();
	}

	public static IParty getParty(CommandContext<CommandSourceStack> stack, String name,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {
		String result = stack.getArgument(name, String.class);
		Optional<IParty> optional = IPartySystem.get(stack.getSource().getLevel()).getPartyByName(result);
		return optional.orElseThrow(() -> exception.create(result));
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		return reader.readQuotedString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			// System.out.println(PartySystemClient.instance());
			if (PartySystemClient.instance().isPresent()) {
				IPartySystem system = PartySystemClient.instance().get();
				system.allDeities().forEach((deity) -> builder.suggest("\"" + deity.uniqueName() + "\"",
						deity.descriptiveName().orElse(ModUtils.literal("deity"))));
				system.nonDeityParties().stream().sorted((x, y) -> Boolean.compare(x.isGroup(), y.isGroup()))
						.forEach((party) -> builder.suggest("\"" + party.uniqueName() + "\"",
								party.descriptiveName().orElse(ModUtils.literal(
										party.isGroup() ? "group" : (party.isEntity() ? "entity" : "unknown")))));
				return builder.buildFuture();
			} else {
				return builder.buildFuture();
			}
		} else {
			return builder.buildFuture();
		}
	}

	@Override
	public Collection<String> getExamples() {
		return Set.of("<deity name>", "village[chunkCoordX, chunkCoordY, dimension]", "<uuid>");
	}

}
