package com.gm910.sotdivine.events;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.deities_and_parties.deity.IDeity;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.deities_and_parties.deity.emanation.spell.ISpellTargetInfo;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.ISphere;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.independent.ItemGenreProvider;
import com.gm910.sotdivine.deities_and_parties.deity.symbol.DeitySymbols;
import com.gm910.sotdivine.deities_and_parties.system_storage.IPartySystem;
import com.gm910.sotdivine.events.custom.EmanationEvent;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldEvents {

	private static final Logger LOGGER = LogUtils.getLogger();

	@SubscribeEvent
	public static void loadEvent(LevelEvent.Load event) {

		/**
		 * Set<Supplier<BannerPattern>> divinePatterns =
		 * DeitySymbols.instance().getDeitySymbolMap().values().stream() .filter((x) ->
		 * x.bannerPattern().isBound()).<Supplier<BannerPattern>>map((x) ->
		 * x.bannerPattern()::get) .collect(Collectors.toSet()); LOGGER.info("Loaded tag
		 * with patterns: {}",
		 * divinePatterns.stream().map(Supplier::get).collect(StreamUtils.setStringCollector(",")));
		 * event.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).tags()
		 * .addOptionalTagDefaults(ModBannerPatternTags.DIVINE_TAG, divinePatterns);
		 */
	}

	@SubscribeEvent
	public static void tickEvent(LevelTickEvent.Post event) {
		if (event.level instanceof ServerLevel level) {
			IPartySystem system = IPartySystem.get(level);
			for (IDeity deity : system.allDeities()) {
				deity.tick(level, level.getGameTime());
			}
			system.markDirty(level);
		}
	}

	/**
	 * All purpose living tick event
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void updateEvent(LivingTickEvent event) {

		if (event.getEntity().level() instanceof ServerLevel level1) {
			if (event.getEntity() instanceof ServerPlayer player) {
				IPartySystem system = IPartySystem.get(level1);

				for (ItemEntity item : (level1).getEntities(EntityTypeTest.forClass(ItemEntity.class),
						player.getBoundingBox().inflate(64), Entity::isOnFire)) {
					EntityReference<Entity> throwerRef = FieldUtils.getInstanceField("thrower", "n", item);
					if (throwerRef == null)
						continue;
					UUID offererID = throwerRef.getUUID();
					Entity offerer = throwerRef.getEntity(level1, Entity.class);
					LOGGER.debug("Sacrificing " + item.getItem().getDisplayName().getString() + " from hand of "
							+ offerer + " (" + offererID + ")");
					Iterable<Entry<Either<Entity, BlockPos>, IDeity>> bannerIter = () -> system
							.findDeitySymbols(level1, item.blockPosition(), IPartySystem.SYMBOL_SEARCH_RADIUS)
							.iterator();

					LOGGER.debug("{}",
							Iterators.toString(system
									.deitiesBySymbol(
											DeitySymbols.instance().getDeitySymbolMap().get(ModUtils.path("crystal")))
									.iterator()));

					Set<IDeity> alreadyChecked = new HashSet<>(); // in case we have multiple symbols from the same
																	// deity
					Multimap<IDeity, Either<Entity, BlockPos>> accepted = MultimapBuilder.hashKeys().hashSetValues()
							.build();

					// check all things with patterns
					for (Entry<Either<Entity, BlockPos>, IDeity> entry : bannerIter) {
						IDeity deity = entry.getValue();
						Either<Entity, BlockPos> entityOrBanner = entry.getKey();

						if (alreadyChecked.contains(deity)) {
							if (accepted.containsKey(deity)) {
								accepted.put(deity, entityOrBanner);
							}
							continue;
						}
						for (ISphere sphere : deity.spheres()) {
							for (ItemGenreProvider offerType : sphere.getGenres(GenreTypes.OFFERING)) {
								if (offerType.matches(level1, item.getItem())) {
									LogUtils.getLogger().debug("Deity {} accepted item with provider {} ",
											deity.uniqueName(), offerType.report());
									accepted.put(deity, entityOrBanner);
									break;
								}
							}
						}
						alreadyChecked.add(deity);
					}

					for (IDeity deity : accepted.keySet()) {

						if (offerer instanceof ServerPlayer playerOfferer) {
							playerOfferer.sendSystemMessage(Component.translatable("sotd.deity.accept_offering",
									deity.descriptiveName().orElse(null), item.getItem().getDisplayName()));
						}

						item.remove(RemovalReason.KILLED);
						accepted.get(deity).forEach((e) -> e
								.ifLeft((en) -> deity.triggerAnEmanation(DeityInteractionType.SYMBOL_RECOGNITION,
										ISpellTargetInfo.builder().targetEntity(en).build(), 1))
								.ifRight((ps) -> deity.triggerAnEmanation(DeityInteractionType.SYMBOL_RECOGNITION,
										ISpellTargetInfo.builder(deity, level1).targetPos(ps).build(), 1)));
					}
				}
			}

		}
	}

	@SubscribeEvent
	public static void tossEvent(ItemTossEvent event) { // ensure potential offering item is marked as thrown by player
		if (event.getEntity().level() instanceof ServerLevel) {
			event.getEntity().addTag("offering");
			event.getEntity().setThrower(event.getPlayer());
			LOGGER.debug("Dropping " + event.getEntity().getItem() + " from hand of " + event.getPlayer());
		}

	}
}