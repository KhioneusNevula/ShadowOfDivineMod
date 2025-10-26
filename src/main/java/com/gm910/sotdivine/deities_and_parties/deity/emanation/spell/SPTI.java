package com.gm910.sotdivine.deities_and_parties.deity.emanation.spell;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.gm910.sotdivine.deities_and_parties.deity.IDeity;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

public record SPTI(@Nullable IDeity deity, @Nullable ServerLevel level, Optional<UUID> opCaster,
		Optional<EntityReference<Entity>> opTargetEntity, Optional<GlobalPos> opTargetPos) implements ISpellTargetInfo {

	@Override
	public final boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ISpellTargetInfo info) {
			return this.opCaster.equals(info.opCaster()) && this.opTargetEntity.equals(info.opTargetEntity())
					&& this.opTargetPos.equals(info.opTargetPos());
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return opCaster.hashCode() + opTargetEntity.hashCode() + opTargetPos.hashCode();
	}

	@Override
	public final String toString() {
		return "Target" + (opCaster.isEmpty() && opTargetEntity.isEmpty() && opTargetPos.isEmpty() ? "Target(World)"
				: "Target{" + (opCaster.map((x) -> "caster=" + x + ",")).orElse("")
						+ (opTargetEntity.map((x) -> "targetEntity=" + x.getUUID() + ",").orElse(""))
						+ (opTargetPos.map((x) -> "targetPos=" + x).orElse("")) + "}");
	}
}
