package com.gm910.sotdivine.common.command.args.party;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.IPartyLister;
import com.gm910.sotdivine.concepts.parties.IPartyLister.IPartyInfo;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.network.party_system.ClientParties;
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
import net.minecraft.network.chat.Component;

/**
 * Argument of a party's unique identifying name
 * 
 * @author borah
 *
 */
class AbstractPartyArgument implements ArgumentType<IPartyInfo> {

	public static final DynamicCommandExceptionType ERROR_PARTY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.party.invalid", p_308764_));
	public static final DynamicCommandExceptionType ERROR_DEITY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.deity.invalid", p_308764_));

	protected final boolean deity;

	protected AbstractPartyArgument(boolean deity) {
		this.deity = deity;
	}

	public static IParty getParty(CommandContext<CommandSourceStack> stack, String name,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {
		IPartyInfo result = stack.getArgument(name, IPartyInfo.class);
		return IPartySystem.get(stack.getSource().getLevel()).getPartyByName(result.uniqueName())
				.orElseThrow(() -> exception.create(result));
	}

	public static IDeity getDeity(CommandContext<CommandSourceStack> stack, String name,
			DynamicCommandExceptionType exception) throws CommandSyntaxException {
		IPartyInfo result = stack.getArgument(name, IPartyInfo.class);
		return IPartySystem.get(stack.getSource().getLevel()).getPartyByName(result.uniqueName())
				.map((s) -> s instanceof IDeity ? (IDeity) s : (IDeity) null)
				.orElseThrow(() -> exception.create(result));
	}

	/**
	 * If the given party matches the string as per this argument
	 * 
	 * @param party
	 * @param string
	 * @return
	 */
	private boolean matchesParty(IPartyInfo party, String string, boolean displayName) {
		if (displayName) {
			return party.descriptiveName().map(Component::getString).orElse(party.uniqueName()).equals(string);
		}
		return party.uniqueName().matches(string);
	}

	/**
	 * What to suggest for this party
	 * 
	 * @param party
	 * @param string
	 * @return
	 */
	private void makeSuggestion(IPartyInfo party, SuggestionsBuilder builder) {
		if (party.isDeity()) {
			builder.suggest(party.uniqueName(), party.descriptiveName().orElse(TextUtils.literal("deity")));
		} else {
			builder.suggest(party.uniqueName(), party.descriptiveName()
					.orElse(TextUtils.literal(party.isGroup() ? "group" : (party.isEntity() ? "entity" : "unknown"))));
		}
		builder.suggest("\"" + party.descriptiveName().map(Component::getString).orElse(party.uniqueName()) + "\"");
	}

	@Override
	public IPartyInfo parse(StringReader reader) throws CommandSyntaxException {
		String quotedName = null;
		String uniqueName = null;
		int ogCursor = reader.getCursor();
		try {
			quotedName = reader.readQuotedString();
		} catch (CommandSyntaxException e) {
			reader.setCursor(ogCursor);
			final int start = reader.getCursor();
			while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
				reader.skip();
			}
			uniqueName = reader.getString().substring(start, reader.getCursor());
		}
		boolean quoted = quotedName != null;

		String name = quoted ? quotedName : uniqueName;

		if (ClientParties.instance().isPresent()) {
			Stream<? extends IPartyInfo> parties;
			if (deity) {
				parties = ClientParties.instance().get().allDeities().stream();
			} else {
				parties = Streams.stream(ClientParties.instance().get().allParties());
			}
			Optional<IPartyInfo> party = parties.filter((s) -> matchesParty(s, name, quoted))
					.map(IPartyInfo.class::cast).findAny();
			if (party.isEmpty()) {
				throw ERROR_PARTY_INVALID.create(name);
			}
			return party.get();
		} else {
			throw ERROR_PARTY_INVALID.create(name);
		}

	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (ClientParties.instance().isPresent()) {
			IPartyLister system = ClientParties.instance().get();
			system.allDeities().forEach((deity) -> makeSuggestion(deity, builder));
			if (!deity)
				system.nonDeityParties().stream().sorted((x, y) -> Boolean.compare(x.isGroup(), y.isGroup()))
						.forEach((party) -> makeSuggestion(party, builder));
		}
		return builder.buildFuture();
	}

}
