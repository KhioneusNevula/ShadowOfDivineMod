package com.gm910.sotdivine.magic.emanation.types;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.spell.ISpellProperties;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;
import com.google.common.collect.Streams;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public abstract class AbstractEmanation implements IEmanation {

	private Optional<ISpellProperties> properties;
	private boolean targetsEntity;
	private boolean targetsPos;

	public AbstractEmanation(boolean targetsEntity, boolean targetsPos, Optional<ISpellProperties> properties) {
		this.properties = properties;
		this.targetsEntity = targetsEntity;
		this.targetsPos = targetsPos;
	}

	@Override
	public Optional<ISpellProperties> optionalSpellProperties() {
		return properties;
	}

	/**
	 * If this targetInfo has a globalPos or entity, return the level it belongs to
	 * 
	 * @param info
	 * @return
	 */
	protected Optional<ServerLevel> getCorrectLevel(ISpellTargetInfo info) {
		return info.opTargetPos().map((pos) -> info.level().getServer().getLevel(pos.dimension()))
				.or(() -> getEntity(info).map((e) -> (ServerLevel) e.level()));
	}

	protected Optional<Entity> getEntity(ISpellTargetInfo info) {
		return getEntity(info, Entity.class);
	}

	/**
	 * Get the entity in a spellInfo from a server level
	 * 
	 * @param level
	 * @param entityRef
	 * @return
	 */
	protected <T extends Entity> Optional<T> getEntity(ISpellTargetInfo info, @Nullable Class<? super T> clazz) {
		return info.opTargetEntity()
				.flatMap((ref) -> Streams.stream(info.level().getServer().getAllLevels())
						.map((i) -> ref.getEntity(i, Entity.class)).filter(Objects::nonNull)
						.filter((s) -> clazz.isInstance(s)).map((s) -> (T) s).findAny());
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
	protected abstract String emanationName();

	@Override
	public String uniqueName() {
		return this.emanationName() + this.optionalSpellProperties().map(Object::toString).orElse("");
	}

	@Override
	public String toString() {
		return this.emanationName();
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
