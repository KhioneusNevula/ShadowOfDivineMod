package com.gm910.sotdivine.systems.party_system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.networking.PartySystemClient;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.systems.deity.symbol.IDeitySymbol;
import com.gm910.sotdivine.systems.deity.type.IDeity;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.villagers.poi.ModPoiTypes;
import com.google.common.base.Functions;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

/**
 * Capability that stores all parties and also deities
 * 
 * @author borah
 *
 */
@AutoRegisterCapability
public interface IPartySystem {

	public static final int SYMBOL_SEARCH_RADIUS = 10;

	public static final String SAVED_DATA_ID = "sotdivine_parties";

	public static final Codec<IPartySystem> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group( // Define the fields within the instance
			Codec.list(IParty.CODEC).fieldOf("parties").forGetter((ds) -> {
				return new ArrayList<>(ds.nonDeityParties());
			}), Codec.list(IDeity.CODEC).fieldOf("deities").forGetter((ds) -> new ArrayList<>(ds.allDeities())))
			.apply(instance, PartySystem::new));

	public static final SavedDataType<PartySystem> SAVE_TYPE = new SavedDataType<>(SAVED_DATA_ID, PartySystem::new,
			CODEC.xmap((x) -> (PartySystem) x, Functions.identity()), null);

	public static IPartySystem get(ServerLevel level) {
		if (PartySystem.cachedSystems.containsKey(level)) {
			return PartySystem.cachedSystems.get(level);
		}
		SOTDMod.LOGGER
				.debug("[IPartySystem] Retrieving and caching instanceof IPartySystem for world " + level.toString());

		PartySystem obtain = null;
		if (level.dimension().equals(Level.OVERWORLD)) {
			obtain = level.getDataStorage().computeIfAbsent(SAVE_TYPE);
			obtain.allParties().stream().forEach((p) -> p.updateLevelReference(level));
		} else {
			SOTDMod.LOGGER.debug("[IPartySystem] Obtained data indirectly due to not accessing from overworld");
			obtain = level.getServer().overworld().getDataStorage().computeIfAbsent(SAVE_TYPE);
			obtain.allParties().stream().forEach((p) -> p.updateLevelReference(level.getServer().overworld()));
		}
		PartySystem.cachedSystems.put(level, obtain);
		return obtain;
	}

	/**
	 * Return the singular instance (which might not yet be loaded) of the party
	 * system that exists on the client side
	 * 
	 * @return
	 */
	@OnlyIn(Dist.CLIENT)
	public static Optional<IPartySystem> clientInstance() {
		return PartySystemClient.instance();
	}

	/**
	 * Return the party associated with this ID
	 */
	public Optional<IParty> getPartyByName(String id);

	/**
	 * If a certain party exists
	 * 
	 * @param id
	 * @return
	 */
	public default boolean partyExists(String id) {
		return getPartyByName(id).isPresent();
	}

	/**
	 * REturn a deity associated with this id, or an empty optional if the thing
	 * accessed is null or a non-deity
	 * 
	 * @param id
	 * @return
	 */
	public default Optional<IDeity> deityByName(String id) {
		return getPartyByName(id).filter((a) -> a instanceof IDeity).map((a) -> (IDeity) a);
	}

	/**
	 * Returns stream of deities based on a given sphere
	 * 
	 * @param sphere
	 * @return
	 */
	public default Stream<IDeity> deitiesBySphere(ISphere sphere) {
		return allDeities().stream().filter((a) -> a.spheres().contains(sphere));
	}

	/**
	 * Returns stream of deities based on a given banner pattern (calls
	 * {@link #deitiesBySymbol(IDeitySymbol)})
	 * 
	 * @param sphere
	 * @return
	 */
	public default Stream<IDeity> deitiesByPattern(BannerPattern symbol) {
		return DeitySymbols.instance().getFromPattern(symbol).map(this::deitiesBySymbol).orElse(Stream.empty());
	}

	/**
	 * Returns stream of deities based on a given symbol
	 * 
	 * @param sphere
	 * @return
	 */
	public default Stream<IDeity> deitiesBySymbol(IDeitySymbol symbol) {
		return allDeities().stream().filter((a) -> a.symbol().equals(symbol));
	}

	/**
	 * Finds all positions which have banners in the given radius (spherical)
	 * 
	 * @param level
	 * @param checkingRegion
	 * @return
	 */

	public static Stream<BlockPos> findBanners(ServerLevel level, BlockPos pos, int radius) {
		return level.getPoiManager().getInRange(ModPoiTypes.BANNER.getHolder().get()::is, pos, radius, Occupancy.ANY)
				.map((rec) -> rec.getPos());
	}

	/**
	 * Finds all entities holding shields in given radius (and a reference to the
	 * item stack itself)
	 * 
	 * @param level
	 * @param checkingRegion
	 * @return
	 */

	public static Stream<Map.Entry<LivingEntity, ItemStack>> findShields(ServerLevel level, BlockPos pos, int radius) {
		return level
				.getEntitiesOfClass(LivingEntity.class, AABB.encapsulatingFullBlocks(pos, pos).inflate(radius),
						(en) -> en.distanceToSqr(Vec3.atCenterOf(pos)) <= radius * radius)
				.stream()
				.flatMap((en) -> Arrays.stream(InteractionHand.values())
						.filter((hand) -> en.getItemInHand(hand).getItem().equals(Items.SHIELD))
						.map((h) -> en.getItemInHand(h)).map((i) -> Map.entry(en, i)));
	}

	/**
	 * Finds all positions which have banners with a specific deity's symbol in the
	 * given radius (spherical). Calls
	 * {@link #findBanners(ServerLevel, BlockPos, int)}
	 * 
	 * @param level
	 * @param checkingRegion
	 * @return
	 */
	public default Stream<Entry<BlockPos, IDeity>> findDeitySymbolsFromBlocks(ServerLevel level, BlockPos pos,
			int radius) {
		return findBanners(level, pos, radius)
				.flatMap((p) -> DeitySymbols.instance().getFromBlock(level, p).map((ds) -> Map.entry(p, ds))
						.flatMap((pds) -> deitiesBySymbol(pds.getValue()).map((d) -> Map.entry(pds.getKey(), d))));
	}

	/**
	 * returns stream of all entities with the given deity symbol on a held shield.
	 * Calls {@link #findShields(ServerLevel, BlockPos, int)}
	 * 
	 * @param level
	 * @param pos
	 * @param radius
	 * @return
	 */
	public default Stream<Entry<Entity, IDeity>> findDeitySymbolsFromShields(ServerLevel level, BlockPos pos,
			int radius) {

		return findShields(level, pos, radius)
				.flatMap((eni) -> eni.getValue().getComponents().get(DataComponents.BANNER_PATTERNS).layers().stream()
						.flatMap((e) -> deitiesByPattern(e.pattern().get())).map((d) -> Map.entry(eni.getKey(), d)));
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
			int radius) {
		return Streams.concat(
				findDeitySymbolsFromBlocks(level, pos, radius)
						.map((x) -> Map.entry(Either.right(x.getKey()), x.getValue())),
				findDeitySymbolsFromShields(level, pos, radius)
						.map((x) -> Map.entry(Either.left(x.getKey()), x.getValue())));
	}

	/**
	 * Return all parties that aren't deities
	 * 
	 * @return
	 */
	public Collection<IParty> nonDeityParties();

	/**
	 * Return all parties, deity and non-deity
	 * 
	 * @return
	 */
	public Collection<IParty> allParties();

	/**
	 * Return all deities
	 * 
	 * @return
	 */
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

	public String report();

	/**
	 * Return the parties that own a given dimension
	 * 
	 * @param dimension
	 */
	public Stream<IParty> dimensionOwners(ResourceKey<Level> dimension);

	/**
	 * Return all parties which have direct control at a certain position
	 */
	public Stream<IParty> regionOwners(ChunkPos position, ResourceKey<Level> dim);

	/**
	 * Call this when you edited stuff so that it saves properly
	 */
	public void markDirty(ServerLevel level);

}
