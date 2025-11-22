package com.gm910.sotdivine.concepts.parties.party.relation;

import java.util.Optional;
import java.util.UUID;

import com.gm910.sotdivine.concepts.parties.party.resource.IPartyResource;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class PartyMemory implements IPartyMemory {

	protected MemoryType type;
	protected Optional<String> actor = Optional.empty();
	protected Optional<String> otherParty = Optional.empty();
	protected Optional<ItemStack> item = Optional.empty();
	protected Optional<GlobalPos> pos = Optional.empty();
	protected Optional<BlockState> block = Optional.empty();
	protected Optional<EntityType<?>> entityType = Optional.empty();
	protected Optional<CompoundTag> entityTag = Optional.empty();
	public Optional<IPartyResource> resource = Optional.empty();
	public Optional<Integer> resAmt = Optional.empty();

	public PartyMemory(MemoryType type) {
		this.type = type;
	}

	protected PartyMemory(MemoryType type, Optional<String> actor, Optional<String> otherParty,
			Optional<ItemStack> item, Optional<GlobalPos> pos, Optional<BlockState> block,
			Optional<EntityType<?>> etype, Optional<CompoundTag> edata, Optional<IPartyResource> res,
			Optional<Integer> amt) {
		this.type = type;
		this.actor = actor;
		this.otherParty = otherParty;
		this.item = item;
		this.pos = pos;
		this.block = block;
		this.entityType = etype;
		this.entityTag = edata;
		this.resource = res;
		this.resAmt = amt;
	}

	protected PartyMemory(String type, Optional<String> actor, Optional<String> target, Optional<ItemStack> item,
			Optional<EntityType<?>> etype, Optional<CompoundTag> entity, Optional<GlobalPos> pos,
			Optional<BlockState> block, Optional<IPartyResource> res, Optional<Integer> amt) {
		this(MemoryType.valueOf(type), actor, target, item, pos, block, etype, entity, res, amt);
	}

	@Override
	public Optional<String> opActorParty() {
		return actor;
	}

	@Override
	public MemoryType memoryType() {
		return type;
	}

	@Override
	public Optional<String> opTargetParty() {
		return otherParty;
	}

	@Override
	public Optional<ItemStack> opItem() {
		return item;
	}

	@Override
	public Optional<CompoundTag> opEntityTag() {
		return entityTag;
	}

	@Override
	public Optional<GlobalPos> opPos() {
		return pos;
	}

	@Override
	public Optional<BlockState> opBlockState() {
		return block;
	}

	@Override
	public Optional<EntityType<?>> opEntityType() {
		return entityType;
	}

	@Override
	public Optional<IPartyResource> opResource() {
		return resource;
	}

	@Override
	public Optional<Integer> opResourceAmount() {
		return resAmt;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IPartyMemory) {
			IPartyMemory memory = (IPartyMemory) obj;
			return type.equals(memory.memoryType()) && actor.equals(memory.opActorParty())
					&& otherParty.equals(memory.opTargetParty()) && item.equals(memory.opItem())
					&& pos.equals(memory.opPos()) && block.equals(memory.opBlockState())
					&& entityType.equals(memory.opEntityType()) && entityTag.equals(memory.opEntityTag())
					&& resource.equals(memory.opResource()) && resAmt.equals(memory.opResourceAmount());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return type.hashCode() + actor.hashCode() + otherParty.hashCode() + item.hashCode() + pos.hashCode()
				+ block.hashCode() + entityType.hashCode() + entityTag.hashCode() + resource.hashCode()
				+ resAmt.hashCode();
	}

	@Override
	public String toString() {
		return "Memory_" + this.type + "{" + actor.map((x) -> "actorParty=" + x).orElse("")
				+ otherParty.map((x) -> "targetParty=" + x).orElse("") + item.map((x) -> "item=" + x).orElse("")
				+ pos.map((x) -> "rawPosition=" + x).orElse("") + block.map((x) -> "block=" + x).orElse("")
				+ entityType.map((x) -> "entityType=" + x).orElse("")
				+ entityTag.map((x) -> "entityTag=" + x).orElse("") + resource.map((x) -> "resource=" + x).orElse("")
				+ resAmt.map((x) -> "resourceAmount=" + x).orElse("") + "}";
	}

}
