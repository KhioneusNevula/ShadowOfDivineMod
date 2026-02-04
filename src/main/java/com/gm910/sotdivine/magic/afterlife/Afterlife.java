package com.gm910.sotdivine.magic.afterlife;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gm910.sotdivine.Config;
import com.gm910.sotdivine.concepts.parties.party.resource.PartyResourceType;
import com.gm910.sotdivine.concepts.parties.party.resource.type.ISoulResource;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.magic.afterlife.anchors.AfterlifeAnchorType;
import com.gm910.sotdivine.magic.afterlife.anchors.IAfterlifeAnchor;
import com.gm910.sotdivine.magic.afterlife.anchors.IPositionableAnchor;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class Afterlife implements IAfterlife, ICapabilitySerializable<CompoundTag> {

	private Table<IAfterlifeAnchor<?, ?>, Soul, Set<SignificanceType>> anchorAndSoulToSignificanceMap = HashBasedTable
			.create();
	private SetMultimap<IAfterlifeAnchor<?, ?>, Soul> anchorToNonSignificantSoulMap = MultimapBuilder.hashKeys()
			.linkedHashSetValues().build();

	private Map<Soul, ISoulResource> soulToDataMap = new HashMap<>();
	private Multimap<Map.Entry<IAfterlifeAnchor<?, ?>, SignificanceType>, Soul> anchorAndSignificanceToSoulTable = MultimapBuilder
			.hashKeys().hashSetValues().build();

	private Multimap<Soul, IAfterlifeAnchor<?, ?>> soulToAnchorsMap = MultimapBuilder.hashKeys().hashSetValues()
			.build();

	private Multimap<AfterlifeAnchorType<?>, IAfterlifeAnchor<?, ?>> anchorTypeToAnchorMap = MultimapBuilder.hashKeys()
			.hashSetValues().build();

	private Multimap<GlobalPos, IPositionableAnchor<?, ?>> positionToAnchorMap = MultimapBuilder.hashKeys()
			.hashSetValues().build();

	private final LazyOptional<IAfterlife> cached = LazyOptional.of(() -> this);

	private static final Codec<SetMultimap<IAfterlifeAnchor<?, ?>, Soul>> CODEC = CodecUtils.multimapCodecFromList(
			IAfterlifeAnchor.codec(), Soul.CODEC, () -> MultimapBuilder.hashKeys().linkedHashSetValues().build());

	private static final Codec<Table<IAfterlifeAnchor<?, ?>, Soul, Set<SignificanceType>>> CODEC_TABLE = CodecUtils
			.tableCodecAsList(
					CodecUtils
							.cellCodec("anchor", IAfterlifeAnchor.codec(), "soul", Soul.CODEC, "significance",
									Codec.list(CodecUtils.caselessEnumCodec(SignificanceType.class))
											.xmap(ls -> new HashSet<>(ls), se -> new ArrayList<>(se))),
					HashBasedTable::create);

	private static final Codec<Map<Soul, ISoulResource>> MA_CODEC = Codec.unboundedMap(Soul.CODEC,
			PartyResourceType.SOUL.get().codec().codec());

	/**
	 * The world (overworld) with this capability
	 */
	private ServerLevel overworld;

	public Afterlife(ServerLevel overworld) {

		this.overworld = overworld;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == IAfterlife.CAPABILITY) {
			return cached.cast();
		}
		return LazyOptional.empty();
	}

	ServerLevel getOverworld() {
		return overworld;
	}

	@Override
	public CompoundTag serializeNBT(Provider registryAccess) {
		CompoundTag tag = new CompoundTag();
		tag.store("signficant", CODEC_TABLE, anchorAndSoulToSignificanceMap);
		tag.store("nonSignficant", CODEC, anchorToNonSignificantSoulMap);
		tag.store("data", MA_CODEC, soulToDataMap);
		return tag;
	}

	@Override
	public void deserializeNBT(Provider registryAccess, CompoundTag nbt) {
		anchorAndSoulToSignificanceMap = nbt.read("significant", CODEC_TABLE).orElseThrow();
		anchorToNonSignificantSoulMap = nbt.read("nonSignificant", CODEC).orElseThrow();
		soulToDataMap = nbt.read("data", MA_CODEC).orElseThrow();
		this.soulToAnchorsMap.clear();
		this.anchorAndSignificanceToSoulTable.clear();

		Streams.concat(anchorToNonSignificantSoulMap.entries().stream(),
				anchorAndSoulToSignificanceMap.rowMap().entrySet().stream()
						.flatMap((x) -> x.getValue().keySet().stream().map(sr -> Map.entry(x.getKey(), sr))))
				.forEach(s -> {
					soulToAnchorsMap.put(s.getValue(), s.getKey());
					s.getValue().updateAfterlifeReference(this);
					anchorTypeToAnchorMap.put(s.getKey().getAnchorType(), s.getKey());
					if (s.getKey() instanceof IPositionableAnchor<?, ?> t) {
						positionToAnchorMap.put(t.getPosition(), t);
					}
				});
		anchorAndSoulToSignificanceMap.cellSet().forEach(cell -> {
			cell.getValue().forEach(sig -> {
				anchorAndSignificanceToSoulTable.put(Map.entry(cell.getRowKey(), sig), cell.getColumnKey());
			});
		});
	}

	@Override
	public Optional<ISoulResource> getSoulResource(Soul ofID) {
		return Optional.ofNullable(soulToDataMap.get(ofID));
	}

	@Override
	public ISoulResource changeSoulResource(Soul soul, ISoulResource resource) {
		return soulToDataMap.put(soul, resource);
	}

	@Override
	public Collection<IAfterlifeAnchor<?, ?>> ownersOfSoul(Soul soul) {
		return Collections.unmodifiableCollection(soulToAnchorsMap.get(soul));
	}

	@Override
	public <T extends IAfterlifeAnchor<?, ?>> Collection<T> getAnchorsOfType(AfterlifeAnchorType<T> type) {
		return (Collection<T>) this.anchorTypeToAnchorMap.get(type);
	}

	@Override
	public Iterable<Soul> getAllSouls(IAfterlifeAnchor<?, ?> forEntity) {

		return Iterables.concat(significantSouls(forEntity), nonSignificantSouls(forEntity));
	}

	@Override
	public int soulCount(IAfterlifeAnchor<?, ?> forEntity) {
		return significantSouls(forEntity).size() + nonSignificantSouls(forEntity).size();
	}

	@Override
	public boolean addSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul, ISoulResource dat, boolean block) {

		if (dat.isSignificant()) {
			this.addSignificantSoul(anchor, soul, dat, SignificanceType.VICTIM);
			return true;
		} else {
			while (this.anchorToNonSignificantSoulMap.get(anchor).size() >= Config.maxSoulsBeforeReplacement) {
				if (block || Config.maxSoulsBeforeReplacement == 0) {
					LogUtils.getLogger()
							.debug("For anchor " + anchor + ", blocked from adding soul: "
									+ new EntityReference<Entity>(soul.uuid()).getEntity(overworld, Entity.class)
									+ " entity (" + soul.uuid() + ") with info " + dat);
					return false;
				}
				this.removeSoul(anchor, anchorToNonSignificantSoulMap.get(anchor).iterator().next());
			}
			LogUtils.getLogger()
					.debug("For anchor " + anchor + ", adding impermanent soul: "
							+ new EntityReference<Entity>(soul.uuid()).getEntity(overworld, Entity.class) + " entity ("
							+ soul.uuid() + ") with info " + dat);
			this.anchorAndSoulToSignificanceMap.remove(anchor, soul);
			this.anchorToNonSignificantSoulMap.put(anchor, soul);
			this.anchorTypeToAnchorMap.put(anchor.getAnchorType(), anchor);
			if (anchor instanceof IPositionableAnchor<?, ?> posa) {
				this.positionToAnchorMap.put(posa.getPosition(), posa);
			}
			soul.updateAfterlifeReference(this);
		}
		this.soulToDataMap.put(soul, dat);
		return this.soulToAnchorsMap.put(soul, anchor);
	}

	@Override
	public boolean isStoredPersistently(IAfterlifeAnchor<?, ?> anchor, Soul soul) {
		return anchorAndSoulToSignificanceMap.contains(anchor, soul);
	}

	@Override
	public void addSignificantSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul, ISoulResource dat, SignificanceType type) {
		LogUtils.getLogger()
				.debug("For anchor " + anchor + ", adding persistent soul with significance " + type + ": "
						+ new EntityReference<Entity>(soul.uuid()).getEntity(overworld, Entity.class) + " entity ("
						+ soul.uuid() + ") with info " + dat);
		this.anchorToNonSignificantSoulMap.remove(anchor, soul);
		if (this.anchorAndSoulToSignificanceMap.get(anchor, soul) == null) {
			this.anchorAndSoulToSignificanceMap.put(anchor, soul, new HashSet<>());
		}
		this.anchorAndSoulToSignificanceMap.get(anchor, soul).add(type);
		this.anchorAndSignificanceToSoulTable.put(Map.entry(anchor, type), soul);
		this.soulToDataMap.put(soul, dat);
		soulToAnchorsMap.put(soul, anchor);
		this.anchorTypeToAnchorMap.put(anchor.getAnchorType(), anchor);
		if (anchor instanceof IPositionableAnchor<?, ?> posa) {
			this.positionToAnchorMap.put(posa.getPosition(), posa);
		}
		soul.updateAfterlifeReference(this);
	}

	@Override
	public boolean containsSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul) {
		return this.anchorAndSoulToSignificanceMap.contains(anchor, soul)
				|| this.anchorToNonSignificantSoulMap.containsEntry(anchor, soul);
	}

	@Override
	public Set<SignificanceType> getSignificance(IAfterlifeAnchor<?, ?> forKiller, Soul soul) {
		return !anchorAndSoulToSignificanceMap.contains(forKiller, soul) ? Set.of()
				: Collections.unmodifiableSet(anchorAndSoulToSignificanceMap.get(forKiller, soul));
	}

	@Override
	public Collection<Soul> getSignificantSouls(IAfterlifeAnchor<?, ?> forKiller, SignificanceType type) {
		return Collections.unmodifiableCollection(anchorAndSignificanceToSoulTable.get(Map.entry(forKiller, type)));
	}

	@Override
	public void removeSoul(IAfterlifeAnchor<?, ?> anchor, Soul soul) {
		LogUtils.getLogger()
				.debug("Removing soul " + soul + " for anchor " + anchor + "... data: " + soulToDataMap.get(soul));
		this.removeInternal(anchor, soul);
		if (this.soulToAnchorsMap.get(soul).isEmpty()) {
			this.soulToDataMap.remove(soul);
		}
		if (this.soulCount(anchor) <= 0) {
			this.anchorTypeToAnchorMap.remove(anchor.getAnchorType(), anchor);
		}
	}

	@Override
	public void reanchor(IAfterlifeAnchor<?, ?> old, IAfterlifeAnchor<?, ?> newAnchor) {
		this.anchorToNonSignificantSoulMap.removeAll(old).forEach(so -> {
			this.anchorToNonSignificantSoulMap.get(newAnchor).add(so);
			this.soulToAnchorsMap.remove(so, old);
			this.soulToAnchorsMap.put(so, newAnchor);
		});
		this.anchorAndSoulToSignificanceMap.row(old).forEach((so, st) -> {
			this.anchorAndSoulToSignificanceMap.put(newAnchor, so, st);
			this.anchorAndSignificanceToSoulTable.removeAll(Map.entry(old, st));
			this.soulToAnchorsMap.remove(so, old);
			this.soulToAnchorsMap.put(so, newAnchor);
		});
		this.anchorAndSoulToSignificanceMap.row(old).clear();
		this.anchorTypeToAnchorMap.remove(old.getAnchorType(), old);
		if (old instanceof IPositionableAnchor<?, ?> poso) {
			this.positionToAnchorMap.remove(poso.getPosition(), poso);
		}
		if (newAnchor instanceof IPositionableAnchor<?, ?> poso) {
			this.positionToAnchorMap.put(poso.getPosition(), poso);
		}

	}

	@Override
	public boolean destroyAnchor(IAfterlifeAnchor<?, ?> anchor) {
		LogUtils.getLogger().debug("Removing anchor " + anchor);
		boolean[] did = { false };
		Optional.ofNullable(this.anchorAndSoulToSignificanceMap.rowMap().remove(anchor))
				.ifPresent(innerMap -> innerMap.keySet().forEach(so -> {
					did[0] = this.removeInternal(anchor, so) || did[0];
					if (this.soulToAnchorsMap.get(so).isEmpty()) {
						this.soulToDataMap.remove(so);
					}
				}));
		this.anchorToNonSignificantSoulMap.removeAll(anchor).forEach(so -> {
			did[0] = this.removeInternal(anchor, so) || did[0];
			if (this.soulToAnchorsMap.get(so).isEmpty()) {
				this.soulToDataMap.remove(so);
			}
		});
		this.anchorTypeToAnchorMap.remove(anchor.getAnchorType(), anchor);
		if (anchor instanceof IPositionableAnchor<?, ?> posa) {
			this.positionToAnchorMap.remove(posa.getPosition(), posa);
		}
		return did[0];
	}

	@Override
	public Collection<IPositionableAnchor<?, ?>> getAnchors(GlobalPos position) {
		return this.positionToAnchorMap.get(position);
	}

	@Override
	public void removeSignificance(IAfterlifeAnchor<?, ?> killer, Soul soul, SignificanceType type) {
		if (this.anchorAndSoulToSignificanceMap.get(killer, soul) instanceof Set<SignificanceType> set) {
			set.remove(type);
			if (set.isEmpty()) {
				this.removeSoul(killer, soul);
			}
		}
	}

	private boolean removeInternal(IAfterlifeAnchor<?, ?> killer, Soul soul) {
		boolean ret = this.soulToAnchorsMap.remove(soul, killer);
		if (this.anchorAndSoulToSignificanceMap.remove(killer, soul) instanceof Set<SignificanceType> types) {
			types.forEach(type -> this.anchorAndSignificanceToSoulTable.remove(Map.entry(killer, type), soul));
		}
		this.anchorToNonSignificantSoulMap.remove(killer, soul);
		return ret;
	}

	@Override
	public ISoulResource removeSoul(Soul soul) {
		LogUtils.getLogger().debug("Removing soul " + soul + " entirely... data: " + soulToDataMap.get(soul));
		this.soulToAnchorsMap.removeAll(soul).forEach(anchor -> {
			this.removeInternal(anchor, soul);
			if (this.soulCount(anchor) <= 0) {
				this.anchorTypeToAnchorMap.remove(anchor.getAnchorType(), anchor);
			}
		});
		return this.soulToDataMap.remove(soul);
	}

	@Override
	public Optional<Soul> peekSignificantSoul(IAfterlifeAnchor<?, ?> fromKiller) {
		Optional<Soul> resourceO = anchorAndSoulToSignificanceMap.row(fromKiller).keySet().stream().findAny();
		return resourceO.flatMap(rs -> this.getSoulResource(rs).map(ss -> Pair.of(rs, ss))).map(resource -> {
			return resource.getFirst();
		});

	}

	@Override
	public Optional<Soul> peekNonsignificantSoul(IAfterlifeAnchor<?, ?> fromKiller) {
		if (anchorToNonSignificantSoulMap.get(fromKiller).isEmpty())
			return Optional.empty();
		Soul ref = Iterators.getLast(anchorToNonSignificantSoulMap.get(fromKiller).iterator());
		return Optional.of(ref);
	}

	@Override
	public Collection<Soul> significantSouls(IAfterlifeAnchor<?, ?> fromKiller) {
		return Collections.unmodifiableCollection(anchorAndSoulToSignificanceMap.row(fromKiller).keySet());
	}

	@Override
	public Collection<Soul> nonSignificantSouls(IAfterlifeAnchor<?, ?> fromKiller) {
		return Collections.unmodifiableCollection(anchorToNonSignificantSoulMap.get(fromKiller));
	}

	@Override
	public Optional<Entity> extractEntity(Soul soul, ServerLevel level, LifeState toState) {
		return this.getSoulResource(soul).flatMap(sr -> sr.regenerateEntity(level, EntitySpawnReason.NATURAL))
				.map(ena -> {
					if (ena instanceof LivingEntity en) {
						ISoulState status = ISoulState.get(en);
						status.reset();
						this.ownersOfSoul(soul).forEach((uu) -> {
							if (isStoredPersistently(uu, soul)) {
								status.setPersistent(true);
							}
							this.getSignificance(uu, soul).forEach(sig -> {
								status.addAnchor(uu, sig);
							});
						});
						status.changeState(toState);

						LogUtils.getLogger()
								.debug("Constructing being associated with soul " + soul + " as entity " + en);
						return en;
					} else {
						return ena;
					}
				}).map(en -> {
					this.removeSoul(soul);
					return en;
				});
	}

	@Override
	public String toString() {
		return "Afterlife" + this.serializeNBT(overworld.registryAccess()).toString();
	}
}
