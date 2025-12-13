package com.gm910.sotdivine.events;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;

import com.gm910.sotdivine.Config;
import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.party.IParty;
import com.gm910.sotdivine.concepts.parties.party.resource.type.DimensionResource;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.trigger.type.right_click.RightClickTriggerEvent;
import com.gm910.sotdivine.magic.sanctuary.cap.ISanctuaryInfo;
import com.gm910.sotdivine.magic.sanctuary.storage.ISanctuarySystem;
import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ClientboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ServerboundMeditationPacket;
import com.gm910.sotdivine.util.FieldUtils;
import com.google.common.base.Predicates;
import com.mojang.logging.LogUtils;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeityWorldEvents {

	private static final Logger LOGGER = LogUtils.getLogger();

	@SubscribeEvent
	public static void generateDeities(PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			var exp = IMind.get(player);
			for (var ip : exp.getAllImpressions()) {
				ModNetwork.sendToClient(ClientboundImpressionsUpdatePacket.add(ip, exp.getTimetracker(ip)), player);
			}
			IPartySystem system = IPartySystem.get(player.level());
			if (system.getPartyByName(player.getUUID().toString()).isEmpty()) { // add player party
				system.addParty(IParty.createEntity(player, player.getDisplayName()), player.level());
			}
			if (system.allDeities().isEmpty()) {// system.getPartyByName(player.getUUID().toString()).isEmpty()) {
				LogUtils.getLogger()
						.debug("Generating a set of NEW deities for a world that is NEW and NEWLY GENERATED");

				Random random = new Random(new Random(player.level().getSeed()).longs()
						.skip(system.allDeities().size() + 1).findAny().orElse(player.level().getSeed()));
				int deities = 1;
				if (system.allDeities().isEmpty())
					deities = random.nextInt(Spheres.instance().getSphereMap().size(),
							Spheres.instance().getSphereMap().size() * 2);
				for (int mama = 0; mama < deities; mama++) {
					IDeity dimde = IDeity.generateDeity(player.level(), player.getLanguage(), random, system);
					if (dimde != null) {

						if (system.dimensionOwners(Level.NETHER).count() == 0) {
							dimde.setResourceAmount(new DimensionResource(Level.NETHER), 1);
						}
					}
				}
			} else {
				system.markDirty(player.level());
			}
		}
	}

	@SubscribeEvent
	public static void attackEntity(LivingDamageEvent event) {
		if (event.getEntity().level() instanceof ServerLevel && event.getSource().getEntity() instanceof Entity surce) {
			ISanctuaryInfo surceinfo = ISanctuaryInfo.get(surce);
			ISanctuaryInfo attInfo = ISanctuaryInfo.get(event.getEntity());
			if (surceinfo.currentSanctuary().orElse(null) instanceof ISanctuary flamer) {
				attInfo.permitEntryTo(flamer.uniqueName(), (int) Config.sanctuaryPermissionTime);

				new ParticleSpecification(ParticleTypes.END_ROD, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 20, false,
						false)
						.sendParticle((ServerLevel) event.getEntity().level(), event.getEntity().getEyePosition());
			}
		}
	}

	@SubscribeEvent
	public static void tickEvent(LevelTickEvent.Post event) {
		if (event.level instanceof ServerLevel level) {
			IPartySystem system = IPartySystem.get(level);
			for (IDeity deity : system.allDeities()) {
				deity.tick(level, level.getGameTime());
			}
			system.markDirty(level);
			ISanctuarySystem.get(level).tick(level.getGameTime(), level);

		}
	}

	@SubscribeEvent
	public static void clickEventEntity(PlayerInteractEvent.EntityInteract event) {

		if (event.getEntity().level() instanceof ServerLevel level1) {
			if (event.getEntity() instanceof ServerPlayer player) {
				double searchRadius = RitualPatterns.instance().getMaxPatternDiameter();

				Optional<IDeity> deityInfo = IRitual.identifyWinningDeity(level1, event.getPos(), searchRadius, true);

				if (deityInfo.isPresent()) {

					IDeity deity = deityInfo.get();
					LogUtils.getLogger().debug("Winning deity: " + deity + "; " + deity.report(level1));
					if (IRitual.tryDetectAndInitiateAnyRitual(level1, deity, event.getEntity().getUUID(), searchRadius,
							new RightClickTriggerEvent(Optional.of(player), event.getHand(), event.getItemStack()),
							List.of(event.getPos()))) {
						LogUtils.getLogger().debug("Ritual started by deity: " + deity);
						event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
					}

				}
			}

		}
	}

	/**
	 * Block right click evengt
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void clickEvent(PlayerInteractEvent.RightClickBlock event) {

		if (event.getEntity().level() instanceof ServerLevel level1) {
			if (event.getEntity() instanceof ServerPlayer player) {
				double searchRadius = RitualPatterns.instance().getMaxPatternDiameter();
				// LOGGER.debug("Checking click on " +
				// event.getLevel().getBlockState(event.getPos()) + " from hand of "
				// + event.getEntity());

				Optional<IDeity> deityInfo = IRitual.identifyWinningDeity(level1, event.getPos(), searchRadius, true);
				if (deityInfo.isPresent()) {
					IDeity winner = deityInfo.get();
					LogUtils.getLogger().debug("Winning deity of click: " + winner + "; " + winner.report(level1));
					if (IRitual.tryDetectAndInitiateAnyRitual(level1, winner, event.getEntity().getUUID(), searchRadius,
							new RightClickTriggerEvent(Optional.of(player), event.getHand(), event.getItemStack()),
							List.of(event.getPos()))) {
						LogUtils.getLogger().debug("Ritual started by deity: " + winner);
						event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
					}

				}
			}

		}
	}

	/**
	 * Ticks an item is safe from fire burning
	 */
	public static final int FIRE_PROTECTION_TIME = 600;

	/**
	 * All purpose living tick event
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void updateEvent(LivingTickEvent event) {

		if (event.getEntity().level() instanceof ServerLevel level1 && event.getEntity().isAlive()) {

			event.getEntity().getCapability(IMind.CAPABILITY).ifPresent((exp) -> {
				exp.tick();
			});

			if (ISanctuarySystem.get(level1).getSanctuaryAtPos(event.getEntity().blockPosition())
					.orElse(null) instanceof ISanctuary flamer) {
				ISanctuaryInfo.get(event.getEntity()).setCurrentSanctuary(flamer);

				int timeLeft = flamer.timeUntilForbidden(event.getEntity());
				if (timeLeft <= Config.sanctuaryEscapeTime) {
					if (timeLeft <= 0) {
						if (level1.getGameTime() % 20 == 0) {
							event.getEntity().setRemainingFireTicks(24);
							event.getEntity().hurtServer(level1,
									new DamageSource(DamageTypes.EXPLOSION.getOrThrow(level1)), 0.3f);
						}
						event.getEntity().knockback(0.3f, event.getEntity().getViewVector(0.0f).x,
								event.getEntity().getViewVector(0.0f).z);
						new ParticleSpecification(ParticleTypes.ENCHANT, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 12,
								false, false).sendParticle((ServerLevel) level1, event.getEntity().position());
					}
				}

			} else {
				ISanctuaryInfo.get(event.getEntity()).setCurrentSanctuary(null);
			}

			if (event.getEntity() instanceof ServerPlayer player) {

				IPartySystem system = IPartySystem.get(level1);
				int index = -2;
				double searchRadius = RitualPatterns.instance().getMaxPatternDiameter();
				itemLoop: for (ItemEntity item : level1.getEntities(EntityTypeTest.forClass(ItemEntity.class),
						player.getBoundingBox().inflate(searchRadius), Predicates.alwaysTrue())) {
					index += 2;
					EntityReference<Entity> throwerRef = FieldUtils.getInstanceField("thrower", "n", item);
					if (throwerRef == null) {
						continue;
					}
					UUID offererID = throwerRef.getUUID();
					Entity offerer = throwerRef.getEntity(level1, Entity.class);
					if (offerer == null || !system.partyExists(offererID.toString())) {
						continue;
					}

					final String fpKey = "fire_protected";
					CustomData.EMPTY.getUnsafe().remove(fpKey);

					CustomData custDat = CustomData.of(item.get(DataComponents.CUSTOM_DATA).copyTag());
					if (custDat.getUnsafe().contains(fpKey)) {
						if (custDat.getUnsafe().getIntOr(fpKey, 0) > 0) {
							item.setInvulnerable(true);
							item.setNeverPickUp();
							if (level1.getGameTime() % 5 == 0) {
								level1.players().forEach(
										(pp) -> pp.connection.send(new ClientboundRemoveEntitiesPacket(item.getId())));
							}
							custDat.getUnsafe().putInt(fpKey, custDat.getUnsafe().getIntOr(fpKey, 1) - 1);
							item.setComponent(DataComponents.CUSTOM_DATA, custDat);
						} else {
							item.remove(RemovalReason.KILLED);

						}
					}
					if (item.isOnFire()
							|| (level1.getGameTime() - index) % (item.getTags().contains("thrown") ? 20 : 100) == 0) {

						Optional<IDeity> deityInfo = IRitual.identifyWinningDeity(level1, item.blockPosition(),
								searchRadius, false);

						if (deityInfo.isEmpty()) {
						} else {
							if (deityInfo.get().spheres().stream()
									.noneMatch((s) -> s.canOffer(level1, item.getItem()))) {
								continue;
							}
							if (item.isOnFire()) {
								if (!custDat.getUnsafe().contains(fpKey)) {
									custDat.getUnsafe().putInt(fpKey, FIRE_PROTECTION_TIME);
									item.setComponent(DataComponents.CUSTOM_DATA, custDat);
									level1.players().forEach((pp) -> pp.connection
											.send(new ClientboundRemoveEntitiesPacket(item.getId())));
									LogUtils.getLogger().debug(
											"Burning " + item + item.get(DataComponents.CUSTOM_DATA).getUnsafe());
								}
							}
							/*
							IDeity winner = deityInfo.get().getKey();
							Stream<IDeity> remainingDeities = deityInfo.get().getValue();
							LogUtils.getLogger().debug("Winning deity: " + winner + "; " + winner.report(level1));
							LOGGER.debug("Offering " + item.getItem().getDisplayName().getString() + " from hand of "
									+ offerer + " (" + offererID + ")");
							for (IDeity deity : (Iterable<IDeity>) () -> Streams
									.concat(Stream.of(winner), remainingDeities).iterator()) {
								if (IRitual.tryDetectAndInitiateAnyRitual(level1, deity, offererID, searchRadius,
										new ItemOfferingTriggerEvent(level1, item.getItem()),
										List.of(item.blockPosition(), item.blockPosition().below()))) {
									LogUtils.getLogger().debug("Ritual started by deity: " + deity);
									break itemLoop;
								}
							}*/
						}
					}
				}
			}

		}
	}

	@SubscribeEvent
	public static void tossEvent(ItemTossEvent event) { // ensure potential offering item is marked as thrown by player
		if (event.getEntity().level() instanceof ServerLevel) {
			event.getEntity().setThrower(event.getPlayer());
			event.getEntity().addTag("thrown");

			LOGGER.debug("Dropping " + event.getEntity().getItem() + " from hand of " + event.getPlayer());
		}
	}
}