package com.gm910.sotdivine.events;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.deity.sphere.ISphere;
import com.gm910.sotdivine.systems.deity.type.IDeity;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldEvents {

	private static final Logger LOGGER = LogUtils.getLogger();

	@SubscribeEvent
	public static void vanillaEvent(VanillaGameEvent event) {
	}

	@SubscribeEvent
	public static void tickEvent(LevelTickEvent.Post event) {
		if (event.level instanceof ServerLevel level) {
			IPartySystem system = IPartySystem.get(level);
			for (IDeity deity : system.allDeities()) {
				deity.tick(level, level.getGameTime());
			}
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
						player.getBoundingBox().inflate(5), Entity::isOnFire)) {
					EntityReference<Entity> throwerRef = ModUtils.getField("thrower", "n", item);
					if (throwerRef == null)
						continue;
					UUID offererID = throwerRef.getUUID();
					Entity offerer = throwerRef.getEntity(level1, Entity.class);
					LOGGER.debug("Sacrificing " + item.getItem().getDisplayName().getString() + " from hand of "
							+ offerer + " (" + offererID + ")");
					Iterable<Entry<Either<Entity, BlockPos>, IDeity>> bannerIter = () -> system
							.findDeitySymbols(level1, item.blockPosition(), IPartySystem.SYMBOL_SEARCH_RADIUS)
							.iterator();

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
							if (sphere.canOffer(item.getItem())) { // if this is a possible offering

								accepted.put(deity, entityOrBanner);
								break;
							}
						}
						alreadyChecked.add(deity);
					}

					for (IDeity deity : accepted.keySet()) {
						LOGGER.debug("For deity "
								+ deity.descriptiveName().map(Component::getString).orElse(deity.uniqueName())
								+ " this item is accepted");
						if (offerer instanceof ServerPlayer playerOfferer) {
							playerOfferer.sendSystemMessage(Component.translatable("sotd.deity.accept_offering",
									deity.descriptiveName().orElse(null), item.getItem().getDisplayName()));
						}

						item.remove(RemovalReason.KILLED);
						accepted.get(deity).forEach((e) -> eitherEffect(level1, e));
					}
				}
			}

		}
	}

	private static void eitherEffect(ServerLevel level, Either<Entity, BlockPos> effecter) {
		effecter.ifLeft((e) -> shieldEffect(level, e));
		effecter.ifRight((b) -> bannerEffect(level, b));
	}

	private static void shieldEffect(ServerLevel level, Entity shielder) {
		for (ParticleOptions particle : new ParticleOptions[] { ParticleTypes.END_ROD, ParticleTypes.FLAME }) {
			level.sendParticles(particle, shielder.getX(), shielder.getEyeY(), shielder.getZ(), 20, 1f, 1f, 1f, 0.3);
		}
	}

	private static void bannerEffect(ServerLevel level, BlockPos banner) {
		Vec3 pos = banner.getBottomCenter();
		for (ParticleOptions particle : new ParticleOptions[] { ParticleTypes.END_ROD, ParticleTypes.ENCHANT }) {
			level.sendParticles(particle, pos.x, pos.y, pos.z, 20, 1f, 1f, 1f, 0.3);
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