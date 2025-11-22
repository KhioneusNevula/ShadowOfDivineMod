package com.gm910.sotdivine.concepts.parties.system_storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.IPartyLister;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.villagers.poi.ModPoiTypes;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.gm910.sotdivine.concepts.symbol.ISymbolBearer;
import com.gm910.sotdivine.concepts.symbol.impl.ISymbolWearer;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.magic.sphere.ISphere;
import com.google.common.base.Functions;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Capability that stores all parties and also deities
 * 
 * @author borah
 *
 */
public interface IPartySystem extends IPartyLister {

	public static final int SYMBOL_SEARCH_RADIUS = 30;

	public static final String SAVED_DATA_ID = "sotdivine_parties";

	public static final Codec<IPartySystem> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group( // Define the fields within the emanation
			Codec.list(IParty.MAP_CODEC.codec()).fieldOf("parties").forGetter((ds) -> {
				return new ArrayList<>(ds.nonDeityParties());
			}),
			Codec.list(IDeity.MAP_CODEC.codec()).fieldOf("deities").forGetter((ds) -> new ArrayList<>(ds.allDeities())))
			.apply(instance, PartySystem::new));

	public static final SavedDataType<PartySystem> SAVE_TYPE = new SavedDataType<>(SAVED_DATA_ID, PartySystem::new,
			CODEC.xmap((x) -> (PartySystem) x, Functions.identity()), null);

	/**
	 * Returns the party system accessed most recently, if present. Useful if we
	 * have no {@link ServerLevel} reference. However, naturally
	 * {@link IPartySystem#get(ServerLevel)} is preferred categorically
	 * 
	 * @return
	 */
	public static Optional<IPartySystem> getCached() {
		return Optional.ofNullable(PartySystem.mostRecentCached);
	}

	/**
	 * Retrieve an instance of the party system from the given level.
	 * 
	 * @param level
	 * @return
	 */
	public static IPartySystem get(ServerLevel levelg) {
		PartySystem obtain = null;
		ServerLevel level;
		if (levelg.dimension().equals(Level.OVERWORLD)) {
			level = levelg;
		} else {
			level = levelg.getServer().overworld();
		}
		if (PartySystem.cachedSystems.containsKey(level)) {
			obtain = PartySystem.cachedSystems.get(level);
		} else {
			LogUtils.getLogger().debug(
					"[IPartySystem] Retrieving and caching instanceof IPartySystem for world " + level.toString());

			obtain = level.getDataStorage().computeIfAbsent(SAVE_TYPE);
			obtain.allParties().stream().forEach((p) -> p.updateLevelReference(levelg));

			PartySystem.cachedSystems.put(level, obtain);
		}
		PartySystem.mostRecentCached = obtain;
		return obtain;
	}

	@Override
	public Optional<IParty> getPartyByName(String id);

	@Override
	public default Optional<IParty> getPartyByDisplayName(String name) {
		return this.allParties().stream()
				.filter((x) -> x.descriptiveName().filter((m) -> m.getString().equals(name)).isPresent()).findAny();
	}

	@Override
	public default Optional<IDeity> deityByName(String id) {
		return getPartyByName(id).filter((a) -> a instanceof IDeity).map((a) -> (IDeity) a);
	}

	@Override
	public default Stream<IDeity> deitiesBySphere(ISphere sphere) {
		return allDeities().stream().filter((a) -> a.spheres().contains(sphere));
	}

	@Override
	public default Stream<IDeity> deitiesByPattern(BannerPattern symbol) {
		return IPartyLister.super.deitiesByPattern(symbol).map((s) -> (IDeity) s);
	}

	@Override
	public default Stream<IDeity> deitiesBySymbol(IDeitySymbol symbol) {
		return IPartyLister.super.deitiesBySymbol(symbol).map((s) -> (IDeity) s);
	}

	/**
	 * Finds all positions which have banners in the given radius (spherical)
	 * 
	 * @param level
	 * @param checkingRegion
	 * @return
	 */

	public static Stream<BlockPos> findSymbolBlocks(ServerLevel level, BlockPos pos, double radius) {
		return level.getPoiManager().getInRange(ModPoiTypes.SYMBOL_BLOCKS.getHolder().get()::is, pos,
				(int) Math.ceil(radius), Occupancy.ANY).map((rec) -> rec.getPos());
	}

	/**
	 * Finds all positions which have banners with a specific deity's symbol in the
	 * given radius (spherical). Calls
	 * {@link #findSymbolBlocks(ServerLevel, BlockPos, int)}
	 * 
	 * @param level
	 * @param checkingRegion
	 * @return
	 */
	public default Stream<Entry<BlockPos, IDeity>> findDeitySymbolsFromBlocks(ServerLevel level, BlockPos pos,
			double radius) {
		return findSymbolBlocks(level, pos, radius)
				.flatMap((p) -> DeitySymbols.instance().getFromPosition(level, p).map((ds) -> Map.entry(p, ds))
						.flatMap((pds) -> deitiesBySymbol(pds.getValue()).map((d) -> Map.entry(pds.getKey(), d))));
	}

	/**
	 * returns stream of all entities with the given deity symbol on a held shield.
	 * Calls {@link #findSymbolBearers(ServerLevel, BlockPos, int)}
	 * 
	 * @param level
	 * @param pos
	 * @param radius
	 * @return
	 */
	public default Stream<Entry<Entity, IDeity>> findDeitySymbolsFromShields(ServerLevel level, BlockPos pos,
			double radius) {

		return level
				.getEntitiesOfClass(LivingEntity.class, AABB.encapsulatingFullBlocks(pos, pos).inflate(radius),
						(en) -> en.distanceToSqr(Vec3.atCenterOf(pos)) <= radius * radius)
				.stream()
				.flatMap((eni) -> Streams
						.concat(eni.getCapability(ISymbolWearer.CAPABILITY).resolve().stream(),
								eni.getCapability(ISymbolBearer.CAPABILITY).resolve().stream())
						.map((x) -> Map.entry(eni, x)))
				.flatMap((x) -> x.getValue().getSymbols().map((b) -> Map.entry(x.getKey(), b)))
				.flatMap((s) -> deitiesBySymbol(s.getValue()).map((x) -> Map.entry(s.getKey(), x)));
	}

	/**
	 * Finds all items, EITHER blocks OR entities holding shields, with symbols of
	 * the deity
	 * 
	 * @param level
	 * @param pos
	 * @param radius
	 * @return
	 */
	public default Stream<Entry<Either<Entity, BlockPos>, IDeity>> findDeitySymbols(ServerLevel level, BlockPos pos,
			double radius) {
		return Streams.concat(
				findDeitySymbolsFromBlocks(level, pos, radius)
						.map((x) -> Map.entry(Either.right(x.getKey()), x.getValue())),
				findDeitySymbolsFromShields(level, pos, radius)
						.map((x) -> Map.entry(Either.left(x.getKey()), x.getValue())));
	}

	/**
	 * Convert the given set of banner pattern layers to only instrument the given
	 * deity
	 * 
	 * @param oldLayers
	 * @param toDeity
	 * @return
	 */
	public default BannerPatternLayers convertPatternLayers(BannerPatternLayers oldLayers, IDeity toDeity) {
		List<BannerPatternLayers.Layer> layers = new ArrayList<>(oldLayers.layers());
		for (int i = 0; i < layers.size(); i++) {
			BannerPatternLayers.Layer layer = layers.get(i);
			if (this.deitiesByPattern(layer.pattern().get()).noneMatch((x) -> x.equals(toDeity))) {
				layers.set(i, new BannerPatternLayers.Layer(toDeity.symbol().bannerPattern(), layer.color()));
			}
		}
		return new BannerPatternLayers(layers);

	}

	/**
	 * Convert the given positioned banner block's deity patterns to the symbol of
	 * the given deity
	 * 
	 * @param level
	 * @param pos
	 * @param toSymbol
	 * @return true if any conversion occurred
	 */
	public default boolean convertBannerPatterns(ServerLevel level, BlockPos pos, IDeity toDeity) {
		if (level.getBlockEntity(pos) instanceof BlockEntity blockEn) {
			if (blockEn instanceof BannerBlockEntity banner) {
				BannerPatternLayers layers = convertPatternLayers(banner.getPatterns(), toDeity);
				if (!layers.equals(banner.getPatterns())) {
					DataComponentMap data = DataComponentMap.builder().set(DataComponents.BANNER_PATTERNS, layers)
							.build();
					banner.applyComponents(data, DataComponentPatch.EMPTY);
					BlockState curState = level.getBlockState(pos);
					for (ServerPlayer player : level.players()) {
						player.connection.send(banner.getUpdatePacket());
						player.connection.send(new ClientboundBlockUpdatePacket(pos, curState));
					}
					return true;
				}
			} // TODO other kinds of symbol blocks?
		}
		return false;
	}

	/**
	 * Convert the given entity's held shield's deity patterns to the symbol of the
	 * given deity
	 * 
	 * @param level
	 * @param pos
	 * @param toSymbol
	 * @return true if any conversion occurred
	 */
	public default boolean convertShieldPatterns(ServerLevel level, Entity entity, IDeity toDeity) {
		if (entity instanceof LivingEntity liv) {
			for (EquipmentSlot slot : EquipmentSlot.VALUES) {
				ItemStack item = liv.getItemBySlot(slot);
				if (item.getCount() > 0) {
					if (item.getComponents().has(DataComponents.BANNER_PATTERNS)) {
						BannerPatternLayers patterns = item.getComponents().get(DataComponents.BANNER_PATTERNS);
						BannerPatternLayers newLayers = convertPatternLayers(patterns, toDeity);
						if (!newLayers.equals(patterns)) {
							DataComponentMap data = DataComponentMap.builder()
									.set(DataComponents.BANNER_PATTERNS, newLayers).build();
							item.applyComponents(data);
							return true;
						}
					}
				}
			}

		}
		return false;
	}

	/**
	 * Get all rituals of the given deity that instrument the given focus block and
	 * trigger event. Does nto check if offerings are present.
	 * 
	 * @param level
	 * @param focus
	 * @return
	 */
	public default Stream<Entry<IRitual, IRitualPattern>> getMatchingRituals(ServerLevel level, BlockPos focus,
			IDeity deity, IRitualTriggerEvent triggerEvent) {
		return deity.getRituals().stream().filter((d) -> {
			boolean mat = d.trigger().matchesEvent(triggerEvent, d, level);

			return mat;
		}).filter((d) -> {
			var provider = d.symbols().get(d.patterns().getBasePattern().focusSymbol());
			boolean mat = provider.matchesPos(level, focus);
			/**
			 * if (mat) { LogUtils.getLogger() .debug("Matches focuspos (" + provider + "->"
			 * + level.getBlockState(focus) + "): " + d); }
			 */
			return mat;
		}).flatMap((d) -> {
			var matches = d.patterns().allMatches(level, focus, d.symbols());
			/**
			 * if (!matches.isEmpty()) { LogUtils.getLogger().debug("Matches are " +
			 * matches); }
			 */
			return matches.stream().map((p) -> Map.entry(d, p));
		});
	}

	@Override
	public Collection<IParty> nonDeityParties();

	@Override
	public Collection<IParty> allParties();

	@Override
	public Collection<IDeity> allDeities();

	/**
	 * Adds a party, deity or otherwise
	 * 
	 * @param party
	 */
	public void addParty(IParty party, ServerLevel level);

	/**
	 * Removes a party by id
	 * 
	 * @param party
	 * @return
	 */
	public IParty removeParty(String party, ServerLevel level);

	/**
	 * Reprot info about party system
	 * 
	 * @return
	 */
	public String report();

	/**
	 * Return the parties that own a given dimension
	 * 
	 * @param dimension
	 */
	public Stream<IParty> dimensionOwners(ResourceKey<Level> dimension);

	/**
	 * Return all parties which have direct control at a certain rawPosition
	 */
	public Stream<IParty> regionOwners(ChunkPos position, ResourceKey<Level> dim);

	/**
	 * Call this when you edited stuff so that it saves properly
	 */
	public void markDirty(ServerLevel level);

	/**
	 * return the parties this entity is a member of
	 * 
	 * @param entity
	 * @return
	 */
	public default Stream<IParty> getPartiesOf(UniquelyIdentifyable entity) {
		return this.nonDeityParties().stream()
				.filter((p) -> p.memberCollection().contains(new EntityReference<>(entity)));
	}

	/**
	 * return the parties this entity is a member of
	 * 
	 * @param entity
	 * @return
	 */
	public default Stream<IParty> getPartiesOf(UUID entity) {
		return this.nonDeityParties().stream()
				.filter((p) -> p.memberCollection().contains(new EntityReference<>(entity)));
	}

}
