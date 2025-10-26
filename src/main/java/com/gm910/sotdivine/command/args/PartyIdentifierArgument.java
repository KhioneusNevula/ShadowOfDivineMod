package com.gm910.sotdivine.command.args;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.gm910.sotdivine.client.IPartyLister;
import com.gm910.sotdivine.client.IPartyLister.IPartyInfo;
import com.gm910.sotdivine.client.ClientPartyLister;
import com.gm910.sotdivine.deities_and_parties.deity.IDeity;
import com.gm910.sotdivine.deities_and_parties.party.IParty;
import com.gm910.sotdivine.deities_and_parties.system_storage.IPartySystem;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Argument of a party's unique identifying name
 * 
 * @author borah
 *
 */
public class PartyIdentifierArgument implements ArgumentType<String> {

	public static final DynamicCommandExceptionType ERROR_PARTY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.party.invalid", p_308764_));

	private boolean deity;

	private PartyIdentifierArgument(boolean deity) {
		this.deity = deity;
	}

	public static PartyIdentifierArgument deityArgument() {
		return new PartyIdentifierArgument(true);
	}

	public static PartyIdentifierArgument argument() {
		return new PartyIdentifierArgument(false);
	}

	public static IParty getParty(CommandContext<CommandSourceStack> stack, String name,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {
		String result = stack.getArgument(name, String.class);
		Optional<IParty> optional = IPartySystem.get(stack.getSource().getLevel()).getPartyByName(result)
				.or(() -> IPartySystem.get(stack.getSource().getLevel()).getPartyByDisplayName(result));
		return optional.orElseThrow(() -> exception.create(result));
	}

	public static IDeity getDeity(CommandContext<CommandSourceStack> stack, String name,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {
		String result = stack.getArgument(name, String.class);
		Optional<IDeity> optional = IPartySystem.get(stack.getSource().getLevel()).getPartyByName(result)
				.or(() -> IPartySystem.get(stack.getSource().getLevel()).getPartyByDisplayName(result))
				.filter((d) -> d instanceof IDeity).map((d) -> (IDeity) d);
		return optional.orElseThrow(() -> exception.create(result));
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {

		String name = reader.readQuotedString();
		if (ClientPartyLister.instance().isPresent()) {
			if (Streams
					.<IPartyInfo>stream(deity ? (Iterable<IPartyInfo>) ClientPartyLister.instance().get().allDeities()
							: (Iterable<IPartyInfo>) ClientPartyLister.instance().get().allParties())
					.noneMatch((s) -> s.uniqueName().equals(name)
							|| s.descriptiveName().map((x) -> x.getString()).equals(Optional.of(name)))) {
				throw ERROR_PARTY_INVALID.create(name);
			}
		}

		return name;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			// System.out.println(ClientPartyLister.instance());
			if (ClientPartyLister.instance().isPresent()) {
				IPartyLister system = ClientPartyLister.instance().get();
				system.allDeities().forEach((deity) -> builder.suggest("\"" + deity.uniqueName() + "\"",
						deity.descriptiveName().orElse(TextUtils.literal("deity"))));
				if (!deity)
					system.nonDeityParties().stream().sorted((x, y) -> Boolean.compare(x.isGroup(), y.isGroup()))
							.forEach((party) -> builder.suggest("\"" + party.uniqueName() + "\"",
									party.descriptiveName().orElse(TextUtils.literal(
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
		return deity ? Set.of("<deity name>")
				: Set.of("<deity name>", "village[chunkCoordX, chunkCoordY, dimension]", "<uuid>");
	}

}
