package com.gm910.sotdivine.deities_and_parties.deity.emanation.spell;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class SpellProperties implements ISpellProperties {

	private SpellAlignment a;
	private Set<SpellTrait> p;
	private SpellPower d;

	SpellProperties(SpellAlignment alignment, SpellPower power, Optional<? extends Collection<SpellTrait>> traits) {
		this.a = alignment;
		this.d = power;
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

	@Override
	public SpellPower difficulty() {
		return d;
	}

}
