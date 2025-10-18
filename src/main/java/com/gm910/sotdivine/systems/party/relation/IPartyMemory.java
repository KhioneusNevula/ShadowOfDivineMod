package com.gm910.sotdivine.systems.party.relation;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.party.IParty;
import com.gm910.sotdivine.systems.party.resource.IPartyResource;
import com.gm910.sotdivine.systems.party_system.IPartySystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * An emanation of something happening that the deity remembers which impacted
 * their relationship with a Party
 * 
 * @author borah
 *
 */
public interface IPartyMemory {

	public static final Codec<IPartyMemory> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group( // Define the fields within the emanation
			Codec.STRING.fieldOf("type").forGetter((x) -> x.memoryType().toString()),
			Codec.STRING.optionalFieldOf("actor").forGetter((x) -> x.opActorParty()),
			Codec.STRING.optionalFieldOf("targetPartyID").forGetter((x) -> x.opTargetParty()),
			ItemStack.CODEC.optionalFieldOf("item").forGetter(IPartyMemory::opItem),
			EntityType.CODEC.optionalFieldOf("entityType").forGetter((e) -> e.opEntityType()),
			CompoundTag.CODEC.optionalFieldOf("entity").forGetter((e) -> e.opEntityTag()),
			GlobalPos.CODEC.optionalFieldOf("position").forGetter(IPartyMemory::opPos),
			BlockState.CODEC.optionalFieldOf("block").forGetter(IPartyMemory::opBlockState),
			IPartyResource.codec().get().optionalFieldOf("resource").forGetter(IPartyMemory::opResource),
			Codec.INT.optionalFieldOf("resourceAmount").forGetter(IPartyMemory::opResourceAmount)

	).apply(instance, (type, actor, target, item, etype, entity, pos, block, res, am) -> new PartyMemory(type, actor,
			target, item, etype, entity, pos, block, res, am)));

	public static Builder builder(MemoryType type) {
		return new Builder(type);
	}

	/**
	 * Returns the uuid representing the party who did the action in the memory
	 * 
	 * @return
	 */
	public Optional<String> opActorParty();

	/**
	 * Returns the result of {@link #opActorParty()} as a party
	 * 
	 * @param level
	 * @return
	 */
	public default Optional<IParty> getOpActor(ServerLevel level) {
		return opActorParty().flatMap((id) -> IPartySystem.get(level).getPartyByName(id));
	}

	/**
	 * Returns the result of {@link #opTargetParty()} as a party
	 * 
	 * @param level
	 * @return
	 */
	public default Optional<IParty> getOpTargetParty(ServerLevel level) {
		return opTargetParty().flatMap((id) -> IPartySystem.get(level).getPartyByName(id));
	}

	/**
	 * The type of this memory
	 * 
	 * @return
	 */
	public MemoryType memoryType();

	/**
	 * If this memory involved another party, return that party's unique name
	 * 
	 * @return
	 */
	public Optional<String> opTargetParty();

	/**
	 * If this memory involved an item, return the item stack
	 * 
	 * @return
	 */
	public Optional<ItemStack> opItem();

	/**
	 * Uses {@link #opEntityTag()} and {@link #opEntityType()} to construct an
	 * entity to reference
	 * 
	 * @param level
	 * @return
	 */
	public default Optional<Entity> opConstructEntity(ServerLevel level) {
		Optional<Entity> op = opEntityType().map((et) -> et.create(level, EntitySpawnReason.LOAD));
		if (op.isPresent() && opEntityTag().isPresent()) {
			CompoundTag tag = opEntityTag().get();
			Entity en = op.get();
			try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
					en.problemPath(), SOTDMod.LOGGER)) {
				en.load(TagValueInput.create(problemreporter$scopedcollector, level.registryAccess(), tag));
			}
		}
		return op;
	}

	/**
	 * If this memory involved an entity that no longer exists (i.e. one that died),
	 * return the entity's tag
	 * 
	 * @return
	 */
	public Optional<CompoundTag> opEntityTag();

	/**
	 * Paired with {@link #opEntityTag()}; returns the type of the entity in
	 * question
	 * 
	 * @return
	 */
	public Optional<EntityType<?>> opEntityType();

	/**
	 * If this memory involved a position, return it
	 * 
	 * @return
	 */
	public Optional<GlobalPos> opPos();

	/**
	 * If this memory involved a kind of block, return it
	 * 
	 * @return
	 */
	public Optional<BlockState> opBlockState();

	/**
	 * If this memory involved some kind of party resource, return it
	 * 
	 * @return
	 */
	public Optional<IPartyResource> opResource();

	/**
	 * If this memory involved a resource, return the amount of the resource
	 * involved
	 * 
	 * @return
	 */
	public Optional<Integer> opResourceAmount();

	public static class Builder {
		private PartyMemory mem;
		private boolean built;

		private void check() {
			if (built)
				throw new IllegalStateException();
		}

		private Builder(MemoryType type) {
			mem = new PartyMemory(type);
		}

		public Builder actor(String actor) {
			check();
			mem.actor = Optional.of(actor);
			return this;
		}

		public Builder target(String party) {
			check();
			mem.otherParty = Optional.of(party);
			return this;
		}

		public Builder item(ItemStack stack) {
			check();
			mem.item = Optional.of(stack);
			return this;
		}

		public Builder pos(GlobalPos pos) {
			check();
			mem.pos = Optional.of(pos);
			return this;
		}

		public Builder block(BlockState state) {
			check();
			mem.block = Optional.of(state);
			return this;
		}

		public Builder entity(EntityType<?> entity, @Nullable CompoundTag tag) {
			check();
			mem.entityType = Optional.of(entity);
			mem.entityTag = Optional.ofNullable(tag);
			return this;
		}

		public Builder entity(EntityType<?> entity) {
			return entity(entity, null);
		}

		public Builder entity(Entity entity) {
			return entity(entity.getType(), entity.serializeNBT(entity.registryAccess()));
		}

		public Builder resource(IPartyResource res) {
			check();
			mem.resource = Optional.of(res);
			return this;
		}

		public Builder resource(IPartyResource res, int amount) {
			check();
			mem.resource = Optional.of(res);
			mem.resAmt = Optional.of(amount);
			return this;
		}

		public IPartyMemory build() {
			built = true;
			return mem;
		}
	}
}
