package com.gm910.sotdivine.magic.sanctuary.cap;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.magic.sanctuary.type.ISanctuary;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Capability provider of symbol bearer for entities with shields
 */
public class SanctuaryInfo implements ICapabilitySerializable<CompoundTag>, ISanctuaryInfo {

	private Entity entity;
	private Optional<ISanctuary> currentSanctuary;

	private final LazyOptional<ISanctuaryInfo> cached = LazyOptional.of(() -> this);

	/**
	 * Maps unique name of sanctuary to a) the time this was granted permission
	 */
	private Table<String, Permissibility, Pair<Long, Integer>> statusTimes = HashBasedTable.create();

	private static final Codec<Table<String, Permissibility, Pair<Long, Integer>>> CODEC = CodecUtils.tableCodec(
			Codec.STRING, CodecUtils.caselessEnumCodec(Permissibility.class),
			Codec.mapPair(Codec.LONG.fieldOf("start_time"), Codec.INT.fieldOf("duration")).codec());

	public SanctuaryInfo(Entity componentGetter) {
		this.entity = componentGetter;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ISanctuaryInfo.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public Optional<ISanctuary> currentSanctuary() {
		return this.currentSanctuary;
	}

	@Override
	public void setCurrentSanctuary(ISanctuary sanctuary) {
		this.currentSanctuary = Optional.ofNullable(sanctuary);
	}

	private void addStatus(Permissibility perm, String sanctuaryName, int ticks) {
		if (ticks < 0) {
			throw new IllegalArgumentException("Ticks: " + ticks);
		} else if (ticks == 0) {
			return;
		}
		statusTimes.put(sanctuaryName, perm, Pair.of(entity.level().getGameTime(), ticks));
	}

	private void removeStatus(String sanctName, Permissibility perm) {
		statusTimes.remove(sanctName, perm);
	}

	private int getStatus(String sanctName, Permissibility perm) {
		var pair = statusTimes.get(sanctName, perm);
		if (pair == null) {
			return -1;
		}
		long duration = (entity.level().getGameTime() - pair.getFirst());
		int difference = (int) (pair.getSecond() - duration);
		if (difference <= 0) {
			statusTimes.remove(sanctName, perm);
			return 0;
		}
		return difference;
	}

	@Override
	public boolean recentlyBanned(String sanct) {
		var ban = statusTimes.get(sanct, Permissibility.BAN);
		var perm = statusTimes.get(sanct, Permissibility.PERMIT);
		if (ban == null)
			return false;
		if (perm == null)
			return true;
		return ban.getFirst() > perm.getFirst();
	}

	@Override
	public boolean recentlyPermitted(String sanct) {
		var ban = statusTimes.get(sanct, Permissibility.BAN);
		var perm = statusTimes.get(sanct, Permissibility.PERMIT);
		if (perm == null)
			return false;
		if (ban == null)
			return true;
		return perm.getFirst() >= ban.getFirst();
	}

	@Override
	public void permitEntryTo(String sanctuaryName, int ticks) {
		addStatus(Permissibility.PERMIT, sanctuaryName, ticks);
	}

	@Override
	public void revokePermission(String sanctuaryName) {
		removeStatus(sanctuaryName, Permissibility.PERMIT);
	}

	@Override
	public int permissionTime(String sanctuaryName) {
		return getStatus(sanctuaryName, Permissibility.PERMIT);
	}

	@Override
	public void banFrom(String sanctuaryName, int ticks) {
		addStatus(Permissibility.BAN, sanctuaryName, ticks);

	}

	@Override
	public int banTime(String sanctuaryName) {
		return getStatus(sanctuaryName, Permissibility.BAN);
	}

	@Override
	public void liftBan(String sanctuaryName) {
		removeStatus(sanctuaryName, Permissibility.BAN);
	}

	@Override
	public void deserializeNBT(Provider registryAccess, CompoundTag nbt) {
		if (nbt.contains("permissions")) {
			DataResult<Pair<Table<String, Permissibility, Pair<Long, Integer>>, Tag>> decoded = CODEC
					.decode(registryAccess.createSerializationContext(NbtOps.INSTANCE), nbt);
			if (decoded.isError()) {
				throw new NbtException(decoded.toString());
			}
			statusTimes = HashBasedTable.create(decoded.result().get().getFirst());
		}
	}

	@Override
	public CompoundTag serializeNBT(Provider registryAccess) {
		var result = CODEC.encode(statusTimes, registryAccess.createSerializationContext(NbtOps.INSTANCE),
				new CompoundTag());
		if (result.isError()) {
			throw new NbtException(result.toString());

		}
		CompoundTag tag = new CompoundTag();
		tag.put("permissions", result.result().get());
		return tag;
	}

	private static enum Permissibility {
		PERMIT, BAN
	}

}
