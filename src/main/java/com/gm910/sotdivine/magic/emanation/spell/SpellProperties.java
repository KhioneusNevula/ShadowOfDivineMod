package com.gm910.sotdivine.magic.emanation.spell;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class SpellProperties implements ISpellProperties {

	private SpellAlignment a;
	private Set<SpellTrait> t;
	private SpellPower d;

	SpellProperties(SpellAlignment alignment, SpellPower power, Optional<? extends Collection<SpellTrait>> traits) {
		this.a = alignment;
		this.d = power;
		this.t = traits.map((t) -> ImmutableSet.copyOf(t)).orElse(ImmutableSet.of());
	}

	@Override
	public SpellAlignment alignment() {
		return a;
	}

	@Override
	public Set<SpellTrait> traits() {
		return t;
	}

	@Override
	public SpellPower difficulty() {
		return d;
	}

	@Override
	public String toString() {
		return "{alignment=" + a + ",difficulty=" + d + (t.isEmpty() ? "" : ",traits=" + t) + "}";
	}

}
