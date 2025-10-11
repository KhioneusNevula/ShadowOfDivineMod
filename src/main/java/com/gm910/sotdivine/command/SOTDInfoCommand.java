package com.gm910.sotdivine.command;

import java.util.Optional;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.systems.villagers.ModBrainElements.MemoryModuleTypes;
import com.gm910.sotdivine.util.ModUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.Level;

public class SOTDInfoCommand {

	private static final DynamicCommandExceptionType ERROR_INVALID_DIMENSION = new DynamicCommandExceptionType(
			p_308347_ -> Component.translatableEscape("argument.dimension.invalid", p_308347_));

	private static final DynamicCommandExceptionType ERROR_PARTY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> ModUtils.trans("cmd.party.invalid", p_308764_));

	private static final DynamicCommandExceptionType ERROR_ENTITY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> ModUtils.trans("cmd.entity.invalid", p_308764_));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("sotd").then(Commands.literal("dimension")
				.then(Commands.argument("dimension", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.DIMENSION))
						.executes(p_258233_ -> checkDimension(p_258233_,
								ResourceOrTagKeyArgument.getResourceOrTagKey(p_258233_, "dimension",
										Registries.DIMENSION, ERROR_INVALID_DIMENSION)))))
				.then(Commands.literal("chunk")
						.then(Commands.argument("pos", ColumnPosArgument.columnPos()).executes(
								p_258232_ -> checkChunk(p_258232_, ColumnPosArgument.getColumnPos(p_258232_, "pos")))))
				.then(Commands.literal("parties").executes((command) -> {
					return checkParties(command);
				}))
				.then(Commands.literal("party").then(Commands.literal("info")
						.then(Commands.argument("party", PartyIdentifierArgument.argument())
								.executes(p_258233_ -> partyInfo(p_258233_,
										PartyIdentifierArgument.getParty(p_258233_, "party", ERROR_PARTY_INVALID))))))
				.then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity())
						.executes(context -> entityInfo(context, EntityArgument.getEntity(context, "entity"))))));
	}

	private static int entityInfo(CommandContext<CommandSourceStack> context, Entity entity)
			throws CommandSyntaxException {
		if (entity instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) entity;
			IPartySystem system = IPartySystem.get(context.getSource().getLevel());
			if (mob.getBrain().checkMemory(MemoryModuleTypes.PARTY_ID.get(), MemoryStatus.REGISTERED)) {
				context.getSource().sendSystemMessage(
						Component.literal(mob.getDisplayName().getString() + " (" + mob.getUUID() + ")"));
				Optional<IParty> getParty = mob.getBrain().getMemory(MemoryModuleTypes.PARTY_ID.get())
						.flatMap(system::getPartyByName);
				getParty.ifPresentOrElse(
						(party) -> context.getSource().sendSystemMessage(ModUtils.trans("cmd.entity.showparty",
								party.descriptiveName().orElse(ModUtils.trans("cmd.noname")), party.uniqueName())),
						() -> {
							context.getSource().sendSystemMessage(ModUtils.trans("cmd.entity.noparty"));
						});

				if (mob.getBrain().checkMemory(MemoryModuleTypes.VILLAGE_LEADER.get(), MemoryStatus.REGISTERED)) {
					Optional<EntityReference<LivingEntity>> leader = mob.getBrain()
							.getMemory(MemoryModuleTypes.VILLAGE_LEADER.get());
					Optional<LivingEntity> leaderEntity = leader
							.map((l) -> l.getEntity(context.getSource().getLevel(), LivingEntity.class));
					leaderEntity
							.ifPresentOrElse(
									(en) -> context.getSource().sendSystemMessage(ModUtils.trans(
											"cmd.entity.showleader", en.getDisplayName().getString(), en.getUUID())),
									() -> {
										leader.ifPresentOrElse(
												(le) -> context.getSource().sendSystemMessage(
														ModUtils.trans("cmd.entity.showleaderid", le.getUUID())),
												() -> context.getSource()
														.sendSystemMessage(ModUtils.trans("cmd.entity.noleader")));
									});
				}
				return Command.SINGLE_SUCCESS;
			}
		}
		throw ERROR_ENTITY_INVALID.create(entity);
	}

	private static int partyInfo(CommandContext<CommandSourceStack> command, IParty party) {
		command.getSource().sendSystemMessage(ModUtils.literal(party.report(command.getSource().getLevel())));

		return Command.SINGLE_SUCCESS;
	}

	private static int checkParties(CommandContext<CommandSourceStack> command) {
		for (IParty party : IPartySystem.get(command.getSource().getLevel()).allParties()) {
			command.getSource()
					.sendSystemMessage(ModUtils.trans("cmd.parties.line",
							party.descriptiveName().orElse(ModUtils.trans("cmd.noname")),
							party.descriptiveInfo(command.getSource().getLevel())));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int checkChunk(CommandContext<CommandSourceStack> source, ColumnPos columnPos) {

		ResourceKey<Level> levelKey = source.getSource().getLevel().dimension();

		IPartySystem system = IPartySystem.get(source.getSource().getLevel());

		source.getSource().sendSystemMessage(ModUtils.trans("cmd.owners.list",
				system.regionOwners(columnPos.toChunkPos(), levelKey).collect(Collectors.toSet()).toString()));
		return Command.SINGLE_SUCCESS;
	}

	private static int checkDimension(CommandContext<CommandSourceStack> commandSourceStack,
			ResourceOrTagKeyArgument.Result<Level> result) throws CommandSyntaxException {

		if (result.unwrap().left().isPresent()) {

			ResourceKey<Level> levelKey = result.unwrap().left().get();

			IPartySystem system = IPartySystem.get(commandSourceStack.getSource().getLevel());

			commandSourceStack.getSource().sendSystemMessage(ModUtils.trans("cmd.owners.list",
					system.dimensionOwners(levelKey).collect(Collectors.toSet()).toString()));

			return Command.SINGLE_SUCCESS;
		}

		throw ERROR_INVALID_DIMENSION.create(result.asPrintable());
	}

}