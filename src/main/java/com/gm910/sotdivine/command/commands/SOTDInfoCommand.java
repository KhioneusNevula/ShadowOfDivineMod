package com.gm910.sotdivine.command.commands;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gm910.sotdivine.command.args.GenrePlacementMapArgument;
import com.gm910.sotdivine.command.args.GenreProviderArgument;
import com.gm910.sotdivine.command.args.PartyIdentifierArgument;
import com.gm910.sotdivine.command.args.RitualPatternArgument;
import com.gm910.sotdivine.command.args.SphereArgument;
import com.gm910.sotdivine.systems.deity.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.systems.deity.ritual.pattern.RitualPattern;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;
import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenreItemGiver;
import com.gm910.sotdivine.systems.deity.sphere.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.IGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.systems.deity.sphere.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.systems.villagers.ModBrainElements.MemoryModuleTypes;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SOTDInfoCommand {

	private static final DynamicCommandExceptionType ERROR_INVALID_DIMENSION = new DynamicCommandExceptionType(
			p_308347_ -> Component.translatableEscape("argument.dimension.invalid", p_308347_));

	private static final DynamicCommandExceptionType ERROR_ENTITY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.entity.invalid", p_308764_));

	private static final DynamicCommandExceptionType ERROR_INVALID_GENRE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.invalid", p_308764_));

	private static final DynamicCommandExceptionType ERROR_GENRE_NO_ITEM = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.no_item", p_308764_));

	private static final DynamicCommandExceptionType ERROR_FAILED_GIVE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.fail_give", p_308764_));
	private static final DynamicCommandExceptionType ERROR_FAILED_PLACE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.fail_place", p_308764_));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

		dispatcher.register(Commands.literal("sotd")

				.then(Commands.literal("party")

						.then(Commands.literal("info")
								.then(Commands.argument("party", PartyIdentifierArgument.argument())
										.executes(p_258233_ -> partyInfo(p_258233_,
												PartyIdentifierArgument.getParty(p_258233_, "party",
														PartyIdentifierArgument.ERROR_PARTY_INVALID)))))
						.then(Commands.literal("list").executes((command) -> {
							return checkParties(command);
						})))
				.then(Commands.literal("resource")

						.then(Commands.literal("dimension")
								.executes(stack -> checkDimension(stack, stack.getSource().getLevel().dimension()))
								.then(Commands.argument("dimension", ResourceKeyArgument.key(Registries.DIMENSION))
										.executes(p_258233_ -> checkDimension(p_258233_,
												ResourceKeyArgument.getRegistryKey(p_258233_, "dimension",
														Registries.DIMENSION, ERROR_INVALID_DIMENSION)))))
						.then(Commands.literal("chunk")
								.then(Commands.argument("pos", ColumnPosArgument.columnPos())
										.executes(p_258232_ -> checkChunk(p_258232_,
												ColumnPosArgument.getColumnPos(p_258232_, "pos")))))

				)
				.then(Commands.literal("entity")
						.then(Commands.argument("entity", EntityArgument.entity()).executes(
								context1 -> entityInfo(context1, EntityArgument.getEntity(context1, "entity")))))
				.then(Commands.literal("placer")
						.then(Commands.argument("patterns", RitualPatternArgument.argument())
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(stack -> placePattern(stack,
												RitualPatternArgument.getArgument(stack, "pattern"),
												BlockPosArgument.getBlockPos(stack, "pos"), Map.of()))
										.then(Commands.argument("placeables", new GenrePlacementMapArgument(context))
												.executes(stack -> placePattern(stack,
														RitualPatternArgument.getArgument(stack, "pattern"),
														BlockPosArgument.getBlockPos(stack, "pos"),
														GenrePlacementMapArgument.getArgument(stack, "placeables")))))))
				.then(Commands.literal("genre").then(Commands.literal("give").then(Commands
						.argument("targets", EntityArgument.players())
						.then(Commands.argument("genre", new GenreProviderArgument<>(context))
								.executes((stack) -> giveGenre(stack, EntityArgument.getPlayers(stack, "targets"),
										GenreProviderArgument.getArgument(stack, "genre", IGiveableGenreProvider.class,
												GenreProviderArgument.ERROR_GENRE_UNGIVEABLE))))))
						.then(Commands.literal("place").then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("genre", new GenreProviderArgument<>(context))
										.executes(stack -> placeGenre(stack, BlockPosArgument.getBlockPos(stack, "pos"),
												GenreProviderArgument.getArgument(stack, "genre",
														IPlaceableGenreProvider.class,
														GenreProviderArgument.ERROR_GENRE_UNPLACEABLE))))))
						.then(Commands.literal("list").then(Commands.argument("sphere", SphereArgument.argument())
								.then(Commands.argument("genre_type", ResourceLocationArgument.id())
										.executes(stack -> listGenres(stack, SphereArgument.getSphere(stack, "sphere"),
												ResourceLocationArgument.getId(stack, "genre_type")))))))
				.then(Commands.literal("memory")).then(Commands.literal("relationship"))
				.then(Commands.literal("ritual")));
	}

	private static int listGenres(CommandContext<CommandSourceStack> context, ISphere sphere,
			ResourceLocation resourceLocation) throws CommandSyntaxException {

		IGenreType<?> genre = GenreTypes.getGenreType(resourceLocation);
		if (genre == null) {
			throw ERROR_INVALID_GENRE.create(resourceLocation);
		}
		var genres = sphere.getGenres(genre);
		context.getSource().sendSystemMessage(
				TextUtils.transPrefix("cmd.genre.list.start", genres.size(), resourceLocation, sphere.name()));
		for (Object item : genres) {
			if (item instanceof IGenreProvider p) {
				context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.item", p.report()));
			} else {
				context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.item", item.toString()));
			}
		}
		context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.end"));
		return Command.SINGLE_SUCCESS;
	}

	private static <T extends IGenrePlacer> int placeGenre(CommandContext<CommandSourceStack> context, BlockPos pos,
			IPlaceableGenreProvider<?, T> argument) throws CommandSyntaxException {
		T obtained = argument.generateRandom(context.getSource().getLevel(), Optional.empty());
		LogUtils.getLogger().debug("Attempting to place at " + pos + " emanation of genre " + argument);
		try {
			if (!obtained.tryPlace(context.getSource().getLevel(), pos)) {
				throw ERROR_FAILED_PLACE.create(argument);
			}
			return Command.SINGLE_SUCCESS;
		} catch (Exception e) {
			throw ERROR_FAILED_PLACE.create(e.getMessage());
		}
	}

	private static <T extends IGenreItemGiver> int giveGenre(CommandContext<CommandSourceStack> context,
			Collection<ServerPlayer> players, IGiveableGenreProvider<?, T> argument) throws CommandSyntaxException {
		LogUtils.getLogger().debug("Attempting to give player(s) item from genre " + argument);
		T obtained = argument.generateRandom(context.getSource().getLevel(), Optional.empty());
		LogUtils.getLogger().debug("Obtained creator/stack " + obtained);

		ItemStack stack;
		try {
			stack = obtained.getAsItem(context.getSource().getLevel(),
					BlockPos.containing(context.getSource().getPosition()));
		} catch (Exception e) {
			throw ERROR_FAILED_GIVE.create(e.getMessage());
		}

		if (stack == null || stack.isEmpty()) {
			throw ERROR_GENRE_NO_ITEM.create(argument);
		}

		players.forEach((p) -> {
			if (!p.addItem(stack)) {
				p.level().addFreshEntityWithPassengers(new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), stack));
			}
		});

		return Command.SINGLE_SUCCESS;
	}

	private static int placePattern(CommandContext<CommandSourceStack> context, IRitualPattern pattern,
			BlockPos blockPos, Map<String, IPlaceableGenreProvider<?, ?>> map) throws CommandSyntaxException {

		try {
			pattern.generatePossible(context.getSource().getLevel(), blockPos, map);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		LogUtils.getLogger().debug("Placed patterns: ");
		for (int i = pattern.maxPos().getY(); i >= pattern.minPos().getY(); i--) {
			System.out.println(((RitualPattern) pattern).drawLayer(i));
		}
		return Command.SINGLE_SUCCESS;

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
				getParty.ifPresentOrElse((party) -> context.getSource()
						.sendSystemMessage(TextUtils.transPrefix("cmd.entity.showparty",
								party.descriptiveName().orElse(TextUtils.transPrefix("cmd.noname")),
								party.uniqueName())),
						() -> {
							context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.entity.noparty"));
						});

				if (mob.getBrain().checkMemory(MemoryModuleTypes.VILLAGE_LEADER.get(), MemoryStatus.REGISTERED)) {
					Optional<EntityReference<LivingEntity>> leader = mob.getBrain()
							.getMemory(MemoryModuleTypes.VILLAGE_LEADER.get());
					Optional<LivingEntity> leaderEntity = leader
							.map((l) -> l.getEntity(context.getSource().getLevel(), LivingEntity.class));
					leaderEntity
							.ifPresentOrElse(
									(en) -> context.getSource().sendSystemMessage(TextUtils.transPrefix(
											"cmd.entity.showleader", en.getDisplayName().getString(), en.getUUID())),
									() -> {
										leader.ifPresentOrElse(
												(le) -> context.getSource().sendSystemMessage(
														TextUtils.transPrefix("cmd.entity.showleaderid", le.getUUID())),
												() -> context.getSource().sendSystemMessage(
														TextUtils.transPrefix("cmd.entity.noleader")));
									});
				}
				return Command.SINGLE_SUCCESS;
			}
		}
		throw ERROR_ENTITY_INVALID.create(entity);
	}

	private static int partyInfo(CommandContext<CommandSourceStack> command, IParty party) {
		command.getSource().sendSystemMessage(TextUtils.literal(party.report(command.getSource().getLevel())));

		return Command.SINGLE_SUCCESS;
	}

	private static int checkParties(CommandContext<CommandSourceStack> command) {
		var parties = IPartySystem.get(command.getSource().getLevel()).allParties();
		command.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.start", parties.size()));
		for (IParty party : parties) {
			command.getSource()
					.sendSystemMessage(TextUtils.transPrefix("cmd.list.item",
							TextUtils.transPrefix("cmd.parties.line",
									party.descriptiveName().orElse(TextUtils.transPrefix("cmd.noname")),
									party.descriptiveInfo(command.getSource().getLevel()))));
		}
		command.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.end"));
		return Command.SINGLE_SUCCESS;
	}

	private static int checkChunk(CommandContext<CommandSourceStack> source, ColumnPos columnPos) {

		ResourceKey<Level> levelKey = source.getSource().getLevel().dimension();

		IPartySystem system = IPartySystem.get(source.getSource().getLevel());

		source.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.owners.list",
				system.regionOwners(columnPos.toChunkPos(), levelKey).collect(Collectors.toSet()).toString()));
		return Command.SINGLE_SUCCESS;
	}

	private static int checkDimension(CommandContext<CommandSourceStack> stack, ResourceKey<Level> levelKey)
			throws CommandSyntaxException {

		IPartySystem system = IPartySystem.get(stack.getSource().getLevel());

		stack.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.owners.list",
				system.dimensionOwners(levelKey).collect(Collectors.toSet()).toString()));

		return Command.SINGLE_SUCCESS;

	}

}