package com.gm910.sotdivine.systems.deity.emanation.types;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.systems.deity.emanation.spell.ISpellTargetInfo;
import com.google.common.collect.Streams;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public abstract class AbstractEmanation implements IEmanation {

	private Optional<ISpellProperties> properties;
	private boolean targetsEntity;
	private boolean targetsPos;

	public AbstractEmanation(boolean targetsEntity, boolean targetsPos, @Nullable ISpellProperties properties) {
		this.properties = Optional.ofNullable(properties);
		this.targetsEntity = targetsEntity;
		this.targetsPos = targetsPos;
	}

	@Override
	public Optional<ISpellProperties> optionalSpellProperties() {
		return properties;
	}

	/**
	 * If this targetInfo has a globalPos, return the level it belongs to
	 * 
	 * @param info
	 * @return
	 */
	protected Optional<ServerLevel> getCorrectLevel(ISpellTargetInfo info) {
		return info.opTargetPos().map((pos) -> info.level().getServer().getLevel(pos.dimension()))
				.or(() -> getEntity(info).map((e) -> (ServerLevel) e.level()));
	}

	/**
	 * Get the entity in a spellInfo from a server level
	 * 
	 * @param level
	 * @param entityRef
	 * @return
	 */
	protected Optional<Entity> getEntity(ISpellTargetInfo info) {
		return info.opTargetEntity().flatMap((ref) -> Streams.stream(info.level().getServer().getAllLevels())
				.map((i) -> ref.getEntity(i, Entity.class)).filter(Objects::nonNull).findAny());
	}

	@Override
	public boolean targetsEntity() {
		return targetsEntity;
	}

	@Override
	public boolean targetsPos() {
		return targetsPos;
	}

	/**
	 * Override this to rename the emanation in its toString
	 * 
	 * @return
	 */
	protected String emanationName() {
		return null;
	}

	@Override
	public String toString() {
		String name = emanationName();
		return "Emanation" + (name != null ? "(\"" + name + "\")" : "");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof AbstractEmanation em) {
			return this.targetsEntity == em.targetsEntity && this.targetsPos == em.targetsPos
					&& this.properties.equals(em.properties);
		}
		return false;
	}

}
