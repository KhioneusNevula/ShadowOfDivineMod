package com.gm910.sotdivine.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.GenreTypes;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.trigger.type.incantation.IncantationTriggerEvent;
import com.gm910.sotdivine.magic.sphere.Spheres;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ClientboundTellrawNotificationPacket;
import com.gm910.sotdivine.network.packet_types.ServerboundIncantationChatPacket;
import com.gm910.sotdivine.util.FieldUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Since we have so many moving parts for magic-word world events
 */

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MagicWordEvents {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static void checkMagicWordRitual(ServerboundIncantationChatPacket packet, ServerPlayer player) {
		ServerLevel level = player.level();
		double searchRadius = RitualPatterns.instance().getMaxPatternDiameter();
		BlockPos lookAt = packet.lookAt().map((s) -> s,
				(e) -> Optional.ofNullable(e.getEntity(level, Entity.class)).orElse(player).blockPosition());
		Set<BlockPos> alongVector = new HashSet<>();
		alongVector.add(lookAt);
		Vec3 step = player.getEyePosition();
		BlockPos stepPos = new BlockPos((int) step.x, (int) step.y, (int) step.z);
		alongVector.add(new BlockPos((int) step.x, (int) step.y, (int) step.z));
		for (int it = 0; it <= player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1 && step
				.distanceTo(player.getEyePosition()) <= lookAt.getCenter().distanceTo(player.getEyePosition()); it++) {
			stepPos = new BlockPos((int) step.x, (int) step.y, (int) step.z);
			BlockPos nextStepPos = stepPos;
			Vec3 nextStep = step;
			for (int b = 1; b < 4; b++) {
				Vec3 lookAngle = lookAt.getCenter().subtract(player.getEyePosition()).normalize().multiply(b, b, b);
				nextStep = step.add(lookAngle);
				nextStepPos = new BlockPos((int) nextStep.x, (int) nextStep.y, (int) nextStep.z);
				if (!nextStepPos.equals(stepPos)) {
					break;
				}
			}
			alongVector.add(nextStepPos);

			step = nextStep;
		}
		LOGGER.debug("Checking incantation by " + player + " with word " + packet.magicWord().getString() + " ("
				+ packet.magicWord() + ") at positions: {"
				+ alongVector.stream().map((b) -> "(" + b.toShortString() + ")->(" + level.getBlockState(b) + ")")
						.collect(CollectionUtils.setStringCollector(", "))
				+ "}");
		posloop: for (BlockPos exPos : alongVector) {
			if (player.gameMode() == GameType.CREATIVE) {
				new ParticleSpecification(ParticleTypes.END_ROD, Vec3.ZERO, new Vec3(0.2, 0.2, 0.2), 0, 50, false,
						false).sendParticle(level, exPos.getCenter());
			}
			List<BlockPos> positions = new ArrayList<>();
			positions.add(exPos);

			for (Direction dir : Direction.values()) {
				positions.add(exPos.relative(dir));
			}
			for (BlockPos lookPos : positions) {
				Optional<IDeity> deityInfo = IRitual.identifyWinningDeity(level, lookPos, searchRadius, false);
				if (deityInfo.isPresent()) {
					IDeity winner = deityInfo.get();
					// LogUtils.getLogger().debug("Winning deity: " + winner + "; " +
					// winner.report(level));
					if (IRitual.tryDetectAndInitiateAnyRitual(level, winner, player.getUUID(), searchRadius,
							new IncantationTriggerEvent(packet.magicWord()), List.of(lookPos))) {
						LogUtils.getLogger().debug("Ritual started by deity: " + winner);
						break posloop;
					}

				}
			}
		}
	}

	@SubscribeEvent
	public static void unloadWorld(LevelEvent.Unload event) {
		ClientboundTellrawNotificationPacket.clearAllPackets();
		ServerboundIncantationChatPacket.clearAllPackets();
	}

	@SubscribeEvent
	public static void serverTickEvent(LevelTickEvent.Post event) {
		if (event.level instanceof ServerLevel) {
			Optional<ServerboundIncantationChatPacket> opPacket = ServerboundIncantationChatPacket.popParsedPacket();
			for (; opPacket.isPresent(); opPacket = ServerboundIncantationChatPacket.popParsedPacket()) {
				if (opPacket.get().sender().map((e) -> e.getEntity(event.level, Player.class))
						.orElse(null) instanceof ServerPlayer player) {
					checkMagicWordRitual(opPacket.get(), player);
				}
			}
		}
	}

	@SubscribeEvent
	public static void serverChatEvent(ServerChatEvent event) {
		var optionalPackets = ServerboundIncantationChatPacket.popUnparsedPackets(event.getMessage());
		if (!optionalPackets.isEmpty()) {
			LogUtils.getLogger().debug("Server chat: Received message with " + optionalPackets.size()
					+ " magic word(s) \"" + event.getMessage().getString() + "\" (" + event.getMessage() + ")");
		}
		for (var packet : optionalPackets) {
			doColorEdits(packet.originalMessage(), packet.accessSequence(), packet.magicWord(), packet.wordAsString(),
					packet.componentAsString(), event::setMessage);

			if (packet.sender().map((e) -> e.getEntity(event.getPlayer().level(), Player.class))
					.orElse(null) instanceof ServerPlayer player) {
				checkMagicWordRitual(packet, player);
			}
		}

	}

	@SubscribeEvent(priority = Priority.LOWEST)
	public static void clientChatEvent(ClientChatEvent event) {
		Either<BlockPos, EntityReference<Entity>> either;
		if (Minecraft.getInstance().hitResult instanceof EntityHitResult enhit) {
			either = Either.right(new EntityReference<Entity>(enhit.getEntity()));
		} else {
			either = Either.left(((BlockHitResult) Minecraft.getInstance().hitResult).getBlockPos());
		}
		Spheres.instance().getSphereMap().values().stream().flatMap((s) -> s.getGenres(GenreTypes.MAGIC_WORD).stream())
				.filter((s) -> event.getMessage().toLowerCase().contains(s.translation().getString().toLowerCase()))
				.forEach((m) -> {
					LogUtils.getLogger().debug("Client chat: Detected word " + m.translation().getString() + " ("
							+ m.translation() + ") in message \"" + event.getMessage() + "\"");
					ModNetwork.sendIncantationToServer(Component.literal(event.getMessage()), event.getMessage(),
							List.of(), m.translation(), m.translation().getString(), either, true);
				});

	}

	@SubscribeEvent(priority = Priority.LOWEST)
	public static void clientSystemMessageEvent(SystemMessageReceivedEvent event) {
		Collection<ServerboundIncantationChatPacket> optionalPackets = ClientboundTellrawNotificationPacket
				.popPackets(event.getMessage());
		LOGGER.debug("Received system message \"" + event.getMessage().getString() + "\" (" + event.getMessage()
				+ ") which matches " + optionalPackets.size()
				+ " /tellraw packets; editing message colors and sending it back to server");
		for (var packet : optionalPackets) {
			doColorEdits(packet.originalMessage(), packet.accessSequence(), packet.magicWord(), packet.wordAsString(),
					packet.componentAsString(), event::setMessage);

			ModNetwork.sendToServer(packet);
		}
	}

	@SubscribeEvent
	public static void serverCommandEvent(CommandEvent event) {
		if (event.getException() == null
				&& event.getParseResults().getContext().getSource().getEntity() instanceof Entity source) {
			var context = event.getParseResults().getContext();
			ModUtils.getInstancesOfCommand(event.getParseResults().getContext(), "tellraw").stream()
					.forEach((commanda) -> {
						try {
							EntitySelector selector = (EntitySelector) commanda.getArguments().get("targets")
									.getResult();
							Component component = (Component) commanda.getArguments().get("message").getResult();
							Optional<MutableComponent> resolvedComponent = ComponentUtils
									.updateForEntity(context.getSource(), Optional.of(component), source, 0);
							List<ServerPlayer> players = selector
									.findPlayers(event.getParseResults().getContext().getSource());
							if (!players.isEmpty()) {
								LOGGER.debug("Received tellraw command with argument \"" + component.getString()
										+ "\" (" + component + "); notifying client about this");
								resolvedComponent.ifPresent((com) -> ModNetwork.sendTellrawToClients(com, source,
										List.of(players.getFirst())));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
		}
	}

	private static void doColorEdits(Component message, List<Integer> access, Component magicWord, String wordString,
			@Nullable String componentAsString, Consumer<Component> setTopLevel) {
		MutableComponent containingComponent = (MutableComponent) TextUtils.getComponent(message, access);
		if (componentAsString == null) {
			componentAsString = containingComponent.getString();
		}

		LogUtils.getLogger()
				.debug("Detected word \"" + wordString + "\" (" + magicWord + ") in component \"" + componentAsString
						+ "\" (" + containingComponent + ") with sequence " + access + " in message " + message);

		List<Component> siblingsCopy = new ArrayList<>(containingComponent.getSiblings());
		FieldUtils.setInstanceField("siblings", "d", containingComponent, new ArrayList<>());

		int index = componentAsString.toLowerCase().indexOf(wordString.toLowerCase());
		String firstPart = componentAsString.substring(0, index);
		String coloredPart = componentAsString.substring(index, index + wordString.length());
		String lastPart = componentAsString.substring(index + wordString.length());

		MutableComponent newContainerComponent = Component.literal(firstPart).setStyle(containingComponent.getStyle())
				.append(Component.literal(coloredPart)
						.setStyle(Style.EMPTY.withBold(true).withInsertion(wordString)
								.withColor(TextColor.parseColor("aqua").getOrThrow())))
				.append(Component.literal(lastPart).setStyle(containingComponent.getStyle()));
		for (var sibling : siblingsCopy) {
			newContainerComponent.append(sibling);
		}

		LogUtils.getLogger()
				.debug("Changing magic-word-containing component from \"" + containingComponent.getString() + "\" ("
						+ containingComponent + ")" + " to \"" + newContainerComponent.getString() + "\" ("
						+ newContainerComponent + ")");

		if (access.isEmpty()) {
			setTopLevel.accept(newContainerComponent);
		} else {
			TextUtils.setComponent(message, newContainerComponent, access);
			LogUtils.getLogger().debug("Message is now \"" + message.getString() + "\" (" + message + ")");
		}
	}

}
