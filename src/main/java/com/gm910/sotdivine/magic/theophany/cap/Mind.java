package com.gm910.sotdivine.magic.theophany.cap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.common.effects.ModEffects;
import com.gm910.sotdivine.common.effects.types.MeditationEffect;
import com.gm910.sotdivine.magic.theophany.impression.IImpression;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionHolder;
import com.gm910.sotdivine.magic.theophany.impression.ImpressionType;
import com.gm910.sotdivine.network.ModNetwork;
import com.gm910.sotdivine.network.packet_types.ClientboundImpressionsUpdatePacket;
import com.gm910.sotdivine.network.packet_types.ClientboundMeditationPacket;
import com.gm910.sotdivine.network.packet_types.ServerboundImpressionsUpdatePacket;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.DynamicOps;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Capability provider of symbol bearer for entities with shields
 */
public class Mind implements ICapabilitySerializable<CompoundTag>, IMind {

	private LivingEntity entity;
	private Optional<ServerPlayer> asPlayer;

	private Table<ImpressionType<?>, IImpression, ImpressionTimetracker> impressions = HashBasedTable.create();

	private boolean meditating = false;
	private boolean storeMeditation = false;

	private final LazyOptional<IMind> cached = LazyOptional.of(() -> this);

	public Mind(LivingEntity componentGetter) {
		this.entity = componentGetter;
		this.asPlayer = Optional.of(entity).map((s) -> s instanceof ServerPlayer x ? x : null);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == IMind.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public boolean canMeditate() {
		return true;
	}

	@Override
	public boolean isMeditating() {
		return meditating;
	}

	@Override
	public void setMeditating(boolean b) {
		this.meditating = b;
		if (!b)
			this.storeMeditation = false;
	}

	@Override
	public void forceStopMeditating() {
		this.meditating = false;
		asPlayer.ifPresent(play -> ModNetwork.sendToClient(ClientboundMeditationPacket.stopMeditating(), play));
		this.storeMeditation = false;
	}

	@Override
	public void forceStartMeditating() {
		this.meditating = true;
		asPlayer.ifPresent(play -> ModNetwork.sendToClient(ClientboundMeditationPacket.stopMeditating(), play));
		this.storeMeditation = true;

	}

	@Override
	public void tick() {
		Set<IImpression> removals = new HashSet<>();
		for (var cell : impressions.cellSet()) {
			if (cell.getValue().lifetime() >= 0
					&& entity.level().getGameTime() - cell.getValue().timeAdded() > cell.getValue().lifetime()) {
				removals.add(cell.getColumnKey());
			}
		}
		removals.forEach((i) -> removeImpression(i));
		if (meditating) {
			entity.addEffect(new MobEffectInstance(ModEffects.MEDITATING.getHolder().get(), MeditationEffect.USUAL_TIME,
					1, false, true, false));

		}
	}

	@Override
	public void updateServer(ServerboundImpressionsUpdatePacket pkg) {
		switch (pkg.action()) {
		case ACTIVATE:
			if (getTimetracker(pkg.holder().get().impression()) instanceof ImpressionTimetracker tt) {

				pkg.holder().get().impression().activate((ServerLevel) entity.level(), entity,
						pkg.holder().get().inputs(), tt);
				removeImpression(pkg.holder().get().impression());
			}
			break;
		case REMOVE:
			removeImpression(pkg.holder().get().impression());
			break;
		}
	}

	@Override
	public Collection<IImpression> getAllImpressions() {
		return impressions.columnKeySet();
	}

	@Override
	public <T extends IImpression> Set<T> getImpressions(ImpressionType<T> type) {
		return (Set<T>) impressions.row(type).keySet();
	}

	@Override
	public ImpressionTimetracker getTimetracker(IImpression impression) {
		return impressions.get(impression.getImpressionType(), impression);
	}

	@Override
	public void addImpression(IImpression impression, ImpressionTimetracker instance) {
		if (!(impressions.put(impression.getImpressionType(), impression, instance) instanceof ImpressionTimetracker tp)
				|| !tp.equals(instance)) {
			asPlayer.ifPresent(
					(p) -> ModNetwork.sendToClient(ClientboundImpressionsUpdatePacket.add(impression, instance), p));
		}
	}

	@Override
	public void removeImpression(IImpression impression) {
		ImpressionTimetracker inst = impressions.remove(impression.getImpressionType(), impression);
		if (inst != null)
			asPlayer.ifPresent(
					(p) -> ModNetwork.sendToClient(ClientboundImpressionsUpdatePacket.remove(impression), p));
	}

	@Override
	public void clearImpressions() {
		impressions.clear();
		asPlayer.ifPresent((p) -> ModNetwork.sendToClient(ClientboundImpressionsUpdatePacket.clear(), p));
	}

	@Override
	public void deserializeNBT(Provider registryAccess, CompoundTag nbt) {
		impressions.clear();
		DynamicOps<Tag> delegate = registryAccess.createSerializationContext(NbtOps.INSTANCE);
		ListTag list = nbt.getList("impressions").orElseThrow();
		for (Tag tg : list) {
			CompoundTag tag = tg.asCompound().orElseThrow();
			IImpression imp = ImpressionType.resourceCodec().decode(delegate, tag.get("impression")).getOrThrow()
					.getFirst();
			ImpressionTimetracker inf = ImpressionTimetracker.CODEC.decode(delegate, tag.get("info")).getOrThrow()
					.getFirst();
			impressions.put(imp.getImpressionType(), imp, inf);
		}
		if (nbt.contains("meditating")) {
			this.storeMeditation = true;
			this.meditating = nbt.getBooleanOr("meditating", this.meditating);
			if (meditating) {
				this.forceStartMeditating();
			}
		}
	}

	@Override
	public CompoundTag serializeNBT(Provider registryAccess) {

		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		DynamicOps<Tag> delegate = registryAccess.createSerializationContext(NbtOps.INSTANCE);

		for (var cell : impressions.cellSet()) {
			CompoundTag cellTag = new CompoundTag();
			cellTag.put("impression",
					ImpressionType.resourceCodec().encodeStart(delegate, cell.getColumnKey()).getOrThrow());
			cellTag.put("info", ImpressionTimetracker.CODEC.encodeStart(delegate, cell.getValue()).getOrThrow());
			list.add(cellTag);
		}

		tag.put("impressions", list);
		if (this.storeMeditation) {
			tag.putBoolean("meditating", meditating);
		}

		return tag;
	}

}
