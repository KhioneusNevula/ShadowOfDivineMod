package com.gm910.sotdivine.systems.deity.emanation.spell;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class SpellProperties implements ISpellProperties {

	private SpellAlignment a;
	private Set<SpellTrait> p;

	SpellProperties(SpellAlignment alignment, Optional<? extends Collection<SpellTrait>> traits) {
		this.a = alignment;
		this.p = traits.map((t) -> ImmutableSet.copyOf(t)).orElse(ImmutableSet.of());
	}

	@Override
	public SpellAlignment alignment() {
		return a;
	}

	@Override
	public Set<SpellTrait> traits() {
		return p;
	}

}
