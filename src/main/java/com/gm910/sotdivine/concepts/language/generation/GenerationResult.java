package com.gm910.sotdivine.concepts.language.generation;

import java.util.Optional;

public interface GenerationResult {

	public static final GR FAIL = new GR(Optional.empty(), false, 0);

	public static GR failed() {
		return FAIL;
	}

	public static GR success(GeneratedConstituent cons) {
		return new GR(Optional.of(cons), true, cons.words());
	}

	public static GR incomplete(GeneratedConstituent cons) {
		return new GR(Optional.of(cons), false, cons.words());
	}

	public Optional<GeneratedConstituent> constituent();

	public boolean complete();

	String getAsString(boolean showBrackets);

	public int words();
}
