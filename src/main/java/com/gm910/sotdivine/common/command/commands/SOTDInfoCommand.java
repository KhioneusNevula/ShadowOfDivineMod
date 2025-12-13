package com.gm910.sotdivine.common.command.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.common.command.args.GenrePlacementMapArgument;
import com.gm910.sotdivine.common.command.args.GenreProviderArgument;
import com.gm910.sotdivine.common.command.args.ImpressionTypeArgument;
import com.gm910.sotdivine.common.command.args.RitualPatternArgument;
import com.gm910.sotdivine.common.command.args.SphereArgument;
import com.gm910.sotdivine.common.command.args.SymbolArgument;
import com.gm910.sotdivine.common.command.args.party.DeityArgument;
import com.gm910.sotdivine.common.command.args.party.PartyArgument;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.concepts.genres.IGenreType;
import com.gm910.sotdivine.concepts.genres.creator.IGenreItemGiver;
import com.gm910.sotdivine.concepts.genres.creator.IGenrePlacer;
import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IPlaceableGenreProvider;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.magic.emanation.DeityInteractionType;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEffectType;
import com.gm910.sotdivine.magic.ritual.emanate.RitualEmanationTargeter;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPattern;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.properties.RitualQuality;
import com.gm910.sotdivine.magic.ritual.properties.RitualType;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.gm910.sotdivine.magic.theophany.cap.ImpressionTimetracker;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.gm910.sotdivine.villagers.ModBrainElements.MemoryModuleTypes;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraftforge.server.command.EnumArgument;

public class SOTDInfoCommand {

	private static final DynamicCommandExceptionType ERROR_INVALID_TAG = new DynamicCommandExceptionType(
			p_308347_ -> Component.translatableEscape("argument.nbt.invalid", p_308347_));

	private static final DynamicCommandExceptionType ERROR_INVALID_DIMENSION = new DynamicCommandExceptionType(
			p_308347_ -> Component.translatableEscape("argument.dimension.invalid", p_308347_));

	private static final DynamicCommandExceptionType ERROR_ENTITY_INVALID = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.entity.invalid", p_308764_));
	private static final DynamicCommandExceptionType ERROR_NO_RITUALS = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.rituals.empty", p_308764_));

	private static final DynamicCommandExceptionType ERROR_INVALID_GENRE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.invalid", p_308764_));

	private static final DynamicCommandExceptionType ERROR_GENRE_NO_ITEM = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.no_item", p_308764_));

	private static final DynamicCommandExceptionType ERROR_FAILED_GIVE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.fail_give", p_308764_));
	private static final DynamicCommandExceptionType ERROR_FAILED_PLACE = new DynamicCommandExceptionType(
			p_308764_ -> TextUtils.transPrefix("cmd.genre.fail_place", p_308764_));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

		dispatcher.register(Commands.literal("sotd").requires(Commands.hasPermission(2))

				.then(Commands.literal("party")

						.then(Commands.literal("info").then(Commands.argument("party", PartyArgument.argument())
								.executes(stack -> partyInfo(stack,
										PartyArgument.getParty(stack, "party", PartyArgument.ERROR_PARTY_INVALID)))))
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
				.then(Commands.literal("pattern").then(Commands.literal("place")
						.then(Commands.argument("pattern", RitualPatternArgument.argument())
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(stack -> placePattern(stack,
												RitualPatternArgument.getArgument(stack, "pattern"),
												BlockPosArgument.getBlockPos(stack, "pos"), Map.of()))
										.then(Commands.argument("placeables", new GenrePlacementMapArgument(context))
												.executes(stack -> placePattern(stack,
														RitualPatternArgument.getArgument(stack, "pattern"),
														BlockPosArgument.getBlockPos(stack, "pos"),
														GenrePlacementMapArgument.getArgument(stack, "placeables")))))))
						.then(Commands.literal("info")
								.then(Commands.argument("pattern", RitualPatternArgument.argument())
										.executes(stack -> patternInfo(stack,
												RitualPatternArgument.getArgument(stack, "pattern")))))
						.then(Commands.literal("list").executes(stack -> listPatterns(stack))))
				.then(Commands.literal("genre").then(

						Commands.literal("give")
								.then(Commands.argument("targets", EntityArgument.players())
										.then(Commands.argument("genre", new GenreProviderArgument(context))
												.executes((stack) -> giveGenre(stack,
														EntityArgument.getPlayers(stack, "targets"),
														GenreProviderArgument.getArgument(stack, "genre",
																IGiveableGenreProvider.class,
																GenreProviderArgument.ERROR_GENRE_UNGIVEABLE))))))
						.then(Commands.literal("place").then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("genre", new GenreProviderArgument(context))
										.executes(stack -> placeGenre(stack, BlockPosArgument.getBlockPos(stack, "pos"),
												GenreProviderArgument.getArgument(stack, "genre",
														IPlaceableGenreProvider.class,
														GenreProviderArgument.ERROR_GENRE_UNPLACEABLE))))))
						.then(Commands.literal("list").then(Commands.argument("sphere", SphereArgument.argument())
								.then(Commands.argument("genre_type", ResourceLocationArgument.id())
										.executes(stack -> listGenres(stack, SphereArgument.getSphere(stack, "sphere"),
												ResourceLocationArgument.getId(stack, "genre_type")))))))
				.then(Commands.literal("memory")
						.then(Commands.literal("list").then(Commands.argument("deity", DeityArgument.argument()))))

				.then(Commands.literal("relationship"))
				.then(Commands.literal("ritual").then(Commands.literal("list").then(Commands.argument("deity",
						DeityArgument.argument())
						.executes(stack -> listRituals(stack,
								DeityArgument.getDeity(stack, "deity", DeityArgument.ERROR_DEITY_INVALID), null, null))
						.then(Commands.argument("type", EnumArgument.enumArgument(RitualType.class))
								.executes(stack -> listRituals(stack,
										DeityArgument.getDeity(stack, "deity", DeityArgument.ERROR_DEITY_INVALID),
										stack.getArgument("type", RitualType.class), null))
								.then(Commands.argument("$quality", EnumArgument.enumArgument(RitualQuality.class))
										.executes(stack -> listRituals(stack,
												DeityArgument.getDeity(stack, "deity",
														DeityArgument.ERROR_DEITY_INVALID),
												stack.getArgument("type", RitualType.class),
												stack.getArgument("$quality", RitualQuality.class)))))
						.then(Commands.argument("$quality", EnumArgument.enumArgument(RitualQuality.class))
								.executes(stack -> listRituals(stack,
										DeityArgument.getDeity(stack, "deity", DeityArgument.ERROR_DEITY_INVALID), null,
										stack.getArgument("$quality", RitualQuality.class)))
								.then(Commands.argument("type", EnumArgument.enumArgument(RitualType.class))
										.executes(stack -> listRituals(stack,
												DeityArgument.getDeity(stack, "deity",
														DeityArgument.ERROR_DEITY_INVALID),
												stack.getArgument("type", RitualType.class),
												stack.getArgument("$quality", RitualQuality.class)))))))
						.then(Commands.literal("start"))
						.then(Commands.literal("place").then(Commands.argument("deity", DeityArgument.argument())
								.then(Commands.argument("type", EnumArgument.enumArgument(RitualType.class))
										.then(Commands
												.argument("$quality", EnumArgument.enumArgument(RitualQuality.class))
												.then(Commands.argument("pos", BlockPosArgument.blockPos())
														.executes(stack -> placeRitual(stack,
																BlockPosArgument.getBlockPos(stack, "pos"),
																DeityArgument.getDeity(stack, "deity",
																		DeityArgument.ERROR_DEITY_INVALID),
																stack.getArgument("type", RitualType.class),
																stack.getArgument("$quality", RitualQuality.class),
																false))
														.then(Commands.literal("spawn_offerings")
																.executes(stack -> placeRitual(stack,
																		BlockPosArgument.getBlockPos(stack, "pos"),
																		DeityArgument.getDeity(stack, "deity",
																				DeityArgument.ERROR_DEITY_INVALID),
																		stack.getArgument("type", RitualType.class),
																		stack.getArgument("$quality",
																				RitualQuality.class),
																		true)))))))))
				.then(Commands.literal("emanation").then(Commands.literal("list").then(Commands
						.argument("sphere", SphereArgument.argument())
						.then(Commands.argument("type", EnumArgument.enumArgument(DeityInteractionType.class))
								.executes(stack -> listEmanations(stack, SphereArgument.getSphere(stack, "sphere"),
										stack.getArgument("type", DeityInteractionType.class))))))
						.then(Commands.literal("start")))
				.then(Commands
						.literal("symbol").then(Commands.literal("list").executes(stack -> listSymbols(stack))).then(
								Commands.literal("give").then(Commands.argument("symbol", SymbolArgument.argument())
										.then(Commands.argument("count", IntegerArgumentType.integer()).executes(
												stack -> giveSymbolBP(stack, SymbolArgument.getSymbol(stack, "symbol"),
														IntegerArgumentType.getInteger(stack, "count"), false))
												.then(Commands.literal("shield").executes(
														stack -> giveSymbolBP(
																stack, SymbolArgument.getSymbol(stack, "symbol"),
																IntegerArgumentType.getInteger(stack, "count"), true))))
										.executes(stack -> giveSymbolBP(stack,
												SymbolArgument.getSymbol(stack, "symbol"), 1, false)))
										.then(Commands
												.argument("deity", DeityArgument.argument()).then(
														Commands.argument("count", IntegerArgumentType.integer())
																.executes(stack -> giveSymbolBP(stack,
																		DeityArgument.getDeity(stack, "deity",
																				DeityArgument.ERROR_DEITY_INVALID)
																				.symbol(),
																		1, false))
																.then(Commands.literal("shield")
																		.executes(stack -> giveSymbolBP(stack,
																				DeityArgument.getDeity(stack, "deity",
																						DeityArgument.ERROR_DEITY_INVALID)
																						.symbol(),
																				1, true))))
												.executes(stack -> giveSymbolBP(stack,
														DeityArgument.getDeity(stack, "deity",
																DeityArgument.ERROR_DEITY_INVALID).symbol(),
														1, false)))))
				.then(Commands.literal("sanctuary")
						.then(Commands.literal("list").then(Commands.argument("chunk", ColumnPosArgument.columnPos())))
						.then(Commands.literal("count")))
				.then(Commands.literal("impression").then(Commands.literal("list")).then(Commands.literal("give")
						.then(Commands.argument("targets", EntityArgument.players()).then(Commands
								.argument("type", ImpressionTypeArgument.argument())
								.then(Commands.argument("deity", DeityArgument.argument()).then(Commands
										.argument("duration", IntegerArgumentType.integer(1))
										.executes(stack -> giveImpression(stack,
												EntityArgument.getPlayers(stack, "targets"),
												ImpressionTypeArgument.getType(stack, "type"),
												IntegerArgumentType.getInteger(stack, "duration"),
												DeityArgument.getDeity(stack, "deity", ERROR_ENTITY_INVALID), null))
										.then(Commands.argument("impression definition", NbtTagArgument.nbtTag())
												.executes(stack -> giveImpression(stack,
														EntityArgument.getPlayers(stack, "targets"),
														ImpressionTypeArgument.getType(stack, "type"),
														IntegerArgumentType.getInteger(stack, "duration"),
														DeityArgument.getDeity(stack, "deity", ERROR_ENTITY_INVALID),
														NbtTagArgument.getNbtTag(stack,
																"impression definition")))))))))));
	}

	private static int giveImpression(CommandContext<CommandSourceStack> stack, Collection<ServerPlayer> players,
			ImpressionType<?> type, int duration, IDeity dei, @Nullable Tag def) throws CommandSyntaxException {
		for (ServerPlayer player : players) {
			IImpression imp = null;
			if (def == null)
				imp = type.createTest(player, dei);
			else {
				var res = type.codec()
						.decode(stack.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), def);
				if (res.isError()) {
					throw ERROR_INVALID_TAG.create(def);
				}
				imp = res.result().get().getFirst();
			}
			IMind.get(player).addImpression(imp,
					new ImpressionTimetracker(dei.uniqueName(), player.level().getGameTime(), duration));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int listPatterns(CommandContext<CommandSourceStack> stack) {
		for (Entry<ResourceLocation, IRitualPattern> pattern : RitualPatterns.instance().getPatternMap().entrySet()) {
			stack.getSource()
					.sendSystemMessage(TextUtils.transPrefix("sotd.cmd.list.item",
							TextUtils.transPrefix("sotd.cmd.map.parenthetical", pattern.getKey(),
									TextUtils.transPrefix("sotd.pattern." + pattern.getKey().toLanguageKey()))));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int patternInfo(CommandContext<CommandSourceStack> stack, IRitualPattern p) {
		stack.getSource().sendSystemMessage(TextUtils.transPrefix("sotd.cmd.ritual.info.pattern", p.translate(),
				p.minPos().toShortString(), p.maxPos().toShortString(), p.blockCount(), p.symbols()));
		return Command.SINGLE_SUCCESS;
	}

	private static int giveSymbolBP(CommandContext<CommandSourceStack> stack, IDeitySymbol symbol, int count,
			boolean shield) throws CommandSyntaxException {
		try {
			Method method;
			try {
				method = GiveCommand.class.getDeclaredMethod("giveItem", CommandSourceStack.class, ItemInput.class,
						Collection.class, int.class);
			} catch (NoSuchMethodException | SecurityException e) {
				try {
					method = GiveCommand.class.getDeclaredMethod("a", CommandSourceStack.class, ItemInput.class,
							Collection.class, int.class);
				} catch (NoSuchMethodException | SecurityException e1) {
					throw ERROR_FAILED_GIVE.create(e);
				}
			}
			method.setAccessible(true);
			var itemreg = stack.getSource().registryAccess().lookupOrThrow(Registries.ITEM);
			Holder<Item> item = Items.SHIELD.builtInRegistryHolder();
			var dyes = Lists.newArrayList(DyeColor.values());
			if (!shield) {
				var banners = Lists.newArrayList(itemreg.getTagOrEmpty(ItemTags.BANNERS));
				Collections.shuffle(banners);
				item = banners.getFirst();
			}

			Collections.shuffle(dyes);
			BannerPatternLayers.Layer layer = new Layer(symbol.bannerPattern(), dyes.getFirst());

			ItemInput specificItem = new ItemInput(item, DataComponentPatch.builder()
					.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(List.of(layer))).build());
			int success = 0;
			try {
				success = (int) method.invoke(null, stack.getSource(), specificItem,
						Set.of(stack.getSource().getPlayer()), count);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw ERROR_FAILED_GIVE.create(e);
			}
			return success;
		} catch (Exception e) {
			e.printStackTrace();
			throw ERROR_FAILED_GIVE.create(e);
		}
	}

	private static int listSymbols(CommandContext<CommandSourceStack> stack) {
		for (var entry : DeitySymbols.instance().getDeitySymbolMap().entrySet()) {
			stack.getSource()
					.sendSystemMessage(TextUtils.transPrefix("cmd.list.item", TextUtils.transPrefix(
							"cmd.symbols.line" + (entry.getValue().preferredSpheres().isPresent() ? "_pref" : "")
									+ (entry.getValue().forbiddenSpheres().isPresent() ? "_forb" : "")
									+ (entry.getValue().allowedSpheres().isPresent() ? "_all" : ""),
							entry.getKey(),
							Component.translatableEscape(entry.getValue().bannerPattern().get().translationKey()),
							entry.getValue().preferredSpheres()
									.map((s) -> s.stream().map(Holder::get).map(ISphere::displayName)
											.collect(CollectionUtils.componentCollectorSetStyle()))
									.orElse(Component.empty()),
							entry.getValue().forbiddenSpheres()
									.map((s) -> s.stream().map(Holder::get).map(ISphere::displayName)
											.collect(CollectionUtils.componentCollectorSetStyle()))
									.orElse(Component.empty()),
							entry.getValue().allowedSpheres()
									.map((s) -> s.stream().map(Holder::get).map(ISphere::displayName)
											.collect(CollectionUtils.componentCollectorSetStyle()))
									.orElse(Component.empty()))));
		}
		return Command.SINGLE_SUCCESS;

	}

	private static int placeRitual(CommandContext<CommandSourceStack> stack, BlockPos pos, IDeity deity,
			RitualType type, RitualQuality quality, boolean spawnOfferings) throws CommandSyntaxException {

		var ritList = Lists.newArrayList(deity.getRituals().stream()
				.filter((r) -> r.ritualQuality() == quality && r.ritualType() == type).iterator());
		Collections.shuffle(ritList);
		if (ritList.isEmpty()) {
			throw ERROR_NO_RITUALS.create(deity.descriptiveName().orElse(Component.literal(deity.uniqueName())));
		}
		IRitual ritual = ritList.getFirst();
		IRitualPattern patta = ritual.patterns().getBasePattern();
		System.out.println("Placing ritual pattern: " + patta);
		System.out.println("Symbols of pattern: " + ritual.symbols());
		int placed = placePattern(stack, patta, pos, ritual.symbols());
		if (spawnOfferings) {
			ritual.offerings().entrySet().stream().forEach((entry) -> {
				var list = Lists.newArrayList(entry.getKey().iterator());
				Collections.shuffle(list);
				ItemStack item = list.getFirst().generateRandom(stack.getSource().getLevel(), Optional.empty())
						.getAsItem(stack.getSource().getLevel(), pos);
				if (item != null && !item.isEmpty()) {
					for (int i = 0; i < entry.getValue(); i++) {
						ItemEntity en = new ItemEntity(stack.getSource().getLevel(), pos.getCenter().x,
								pos.above().getBottomCenter().y - 0.2, pos.getCenter().z, item.copy());
						if (stack.getSource().getEntity() != null)
							en.setThrower(stack.getSource().getEntity());
						stack.getSource().getLevel().addFreshEntityWithPassengers(en);
					}
				}
			});
		}
		stack.getSource()
				.sendSystemMessage(TextUtils.transPrefix("sotd.cmd.ritual.info.trigger", ritual.trigger().translate()));
		return placed;
	}

	private static int listEmanations(CommandContext<CommandSourceStack> stack, ISphere sphere,
			DeityInteractionType type) {
		for (IEmanation emanation : sphere.emanationsOfType(type)) {
			stack.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.item", emanation.translate()));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int listRituals(CommandContext<CommandSourceStack> context, IDeity deity,
			@Nullable RitualType ritualType, @Nullable RitualQuality ritualQuality) {
		for (IRitual ritual : deity.getRituals()) {
			if (ritualType != null && ritual.ritualType() != ritualType
					|| ritualQuality != null && ritual.ritualQuality() != ritualQuality) {
				continue;
			}
			context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.item", TextUtils.transPrefix(
					"cmd.ritual.info",
					TextUtils.transPrefix("sotd.cmd.ritual.type." + ritual.ritualType().name().toLowerCase()),
					TextUtils.transPrefix("sotd.cmd.ritual.quality." + ritual.ritualQuality().name().toLowerCase()),
					ritual.symbols().entrySet().stream()
							.map((s) -> Map.entry(Component.literal(s.getKey()), s.getValue().translate()))
							.collect(CollectionUtils.componentCollectorMapStyle()))));
			context.getSource()
					.sendSystemMessage(TextUtils.transPrefix(
							"cmd.ritual.info.2" + (ritual.ritualEffect(RitualEffectType.SUCCESS) == null ? "a" : ""),
							ritual.trigger().translate(),
							ritual.ritualEffect(RitualEffectType.SUCCESS).map(RitualEmanationTargeter::translate)
									.orElse(Component.empty()),
							ritual.emanations().stream().map(IEmanation::translate)
									.collect(CollectionUtils.componentCollectorCommasPretty())));
			context.getSource().sendSystemMessage(
					TextUtils.transPrefix("cmd.ritual.info.3", ritual.offerings().entrySet().stream().map((s) -> {
						var mapac = TextUtils.transPrefix("sotd.cmd.map", s.getKey().stream()
								.map(IGenreProvider::translate).collect(CollectionUtils.componentCollectorSetStyle()),
								s.getValue());
						return mapac;
					}).collect(CollectionUtils.componentCollectorSetStyle()),
							ritual.patterns().getBasePattern().translate()));
		}
		return Command.SINGLE_SUCCESS;
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
				context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.list.item", p.translate()));
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
			LogUtils.getLogger().debug("Placed patterns: ");
			for (int i = pattern.maxPos().getY(); i >= pattern.minPos().getY(); i--) {
				System.out.println("Layer " + i + ":");
				System.out.println(((RitualPattern) pattern).drawLayer(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return Command.SINGLE_SUCCESS;

	}

	private static int entityInfo(CommandContext<CommandSourceStack> context, Entity entity)
			throws CommandSyntaxException {
		if (entity instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) entity;
			IPartySystem system = IPartySystem.get(context.getSource().getLevel());
			Optional<IParty> getParty = Optional.empty();
			if (mob.getBrain().checkMemory(MemoryModuleTypes.PARTY_ID.get(), MemoryStatus.REGISTERED)) {
				context.getSource().sendSystemMessage(
						Component.literal(mob.getDisplayName().getString() + " (" + mob.getUUID() + ")"));
				getParty = mob.getBrain().getMemory(MemoryModuleTypes.PARTY_ID.get()).flatMap(system::getPartyByName);
			} else if (mob instanceof ServerPlayer player) {
				getParty = system.getPartyByName(player.getStringUUID());
			} else {
				throw ERROR_ENTITY_INVALID.create(entity);
			}
			getParty.ifPresentOrElse(
					(party) -> context.getSource().sendSystemMessage(TextUtils.transPrefix("cmd.entity.showparty",
							party.descriptiveName().orElse(TextUtils.transPrefix("cmd.noname")), party.uniqueName())),
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
											() -> context.getSource()
													.sendSystemMessage(TextUtils.transPrefix("cmd.entity.noleader")));
								});
			}
			return Command.SINGLE_SUCCESS;
		}

		throw ERROR_ENTITY_INVALID.create(entity);
	}

	private static int partyInfo(CommandContext<CommandSourceStack> command, IParty party) {
		command.getSource().sendSystemMessage(party.descriptiveInfo(command.getSource().getLevel()));

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