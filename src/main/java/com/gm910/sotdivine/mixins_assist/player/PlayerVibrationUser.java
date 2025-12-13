package com.gm910.sotdivine.mixins_assist.player;

import java.util.List;

import javax.annotation.Nullable;

import com.gm910.sotdivine.common.misc.ParticleSpecification;
import com.gm910.sotdivine.magic.theophany.cap.IMind;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

public class PlayerVibrationUser implements VibrationSystem.User {
	private static final int GAME_EVENT_LISTENER_RANGE = 32;

	public static final TagKey<GameEvent> MEDITATION_INTERRUPTERS = TagKey.create(Registries.GAME_EVENT,
			ModUtils.path("can_interrupt_meditation"));

	private final PositionSource positionSource;
	private Player player;

	public PlayerVibrationUser(Player player) {
		this.player = player;
		this.positionSource = new EntityPositionSource(player, player.getEyeHeight());
	}

	@Override
	public int getListenerRadius() {
		return GAME_EVENT_LISTENER_RANGE;
	}

	@Override
	public PositionSource getPositionSource() {
		return this.positionSource;
	}

	@Override
	public TagKey<GameEvent> getListenableEvents() {
		return MEDITATION_INTERRUPTERS;
	}

	@Override
	public boolean canTriggerAvoidVibration() {
		return true;
	}

	@Override
	public boolean canReceiveVibration(ServerLevel level, BlockPos source, Holder<GameEvent> eventHolder,
			GameEvent.Context ctxt) {
		return !player.isDeadOrDying() && player.isAlive() && level.getWorldBorder().isWithinBounds(source)
				&& IMind.get(player).isMeditating();
	}

	@Override
	public void onReceiveVibration(ServerLevel level, BlockPos sourcePos, Holder<GameEvent> event,
			@Nullable Entity entitySource, @Nullable Entity trueEntitySource, float distance) {
		if (!player.isDeadOrDying()) {
			level.broadcastEntityEvent(player, (byte) 61);
			if (IMind.get(player).isMeditating()) {
				new ParticleSpecification(ParticleTypes.FLASH, Vec3.ZERO, Vec3.ZERO, 0f, 20, true, true)
						.sendParticle((ServerPlayer) player, sourcePos.getCenter());
				String transKey = "meditation.interruption." + event.unwrapKey().get().location().toLanguageKey();
				Component entityTrans = entitySource != null
						? (entitySource.hasCustomName() ? entitySource.getName()
								: Component.translatable("sotd.common_noun", entitySource.getName()))
						: Component.literal("null");
				var block = level.getBlockState(sourcePos).getBlock();
				Component defaultTrans = TextUtils.translationWithFallback(transKey, Component.translatable(
						"meditation.interruption.default", transKey, block.getName(), entityTrans), block.getName());
				Component forBlock = TextUtils.translationWithFallbacks(
						transKey + "." + block.builtInRegistryHolder().key().location().toLanguageKey(),
						block.builtInRegistryHolder().tags()
								.map((s) -> Component.translatableEscape(
										transKey + ".tag." + s.location().toLanguageKey(), block.getName()))
								.toList(),
						defaultTrans, block.getName());
				Component forEntity = TextUtils.translationWithFallbacks(
						transKey + ".entity." + block.builtInRegistryHolder().key().location().toLanguageKey(),
						CollectionUtils.concat(
								block.builtInRegistryHolder().tags()
										.map((s) -> Component.translatableEscape(
												transKey + ".entity.tag." + s.location().toLanguageKey(), entityTrans,
												block.getName()))
										.toList(),
								List.of(Component.translatable(transKey + ".entity", entityTrans, block.getName()))),
						forBlock, entityTrans, block.getName());
				Component forPlayer = TextUtils.translationWithFallbacks(
						transKey + ".self." + block.builtInRegistryHolder().key().location().toLanguageKey(),
						CollectionUtils.concat(
								block.builtInRegistryHolder().tags()
										.map((s) -> Component.translatableEscape(
												transKey + ".self.tag." + s.location().toLanguageKey(),
												block.getName()))
										.toList(),
								List.of(Component.translatable(transKey + ".self", entityTrans, block.getName()))),
						forEntity, entityTrans, block.getName());

				((ServerPlayer) player).displayClientMessage(
						entitySource == player ? forPlayer : (entitySource != null ? forEntity : forBlock), true);

				IMind.get(player).forceStopMeditating();
			}
		}
	}

}