package com.gm910.sotdivine.magic.ritual.properties;

import java.util.Map;

import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;

public class RitualParameters implements IRitualParameters {

	private Map<RitualParameter, Float> map;
	private float agIn;

	public RitualParameters(Map<RitualParameter, Float> params) {
		map = ImmutableMap.copyOf(params);
		agIn = (float) params.values().stream().mapToDouble((s) -> s).sum();
		if (agIn == 0)
			agIn = 1;
	}

	@Override
	public float get(RitualParameter parameter) {
		return map.get(parameter);
	}

	@Override
	public float aggregateIntensity() {
		return agIn;
	}

}
