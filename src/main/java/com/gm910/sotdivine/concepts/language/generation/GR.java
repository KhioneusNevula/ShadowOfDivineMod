package com.gm910.sotdivine.concepts.language.generation;

import java.util.Optional;

/**
 * Result of a language generation
 */
record GR(Optional<GeneratedConstituent> constituent, boolean complete, int words) implements GenerationResult {

	@Override
	public String getAsString(boolean showBrackets) {
		return constituent.isEmpty() ? "FAIL"
				: ((complete ? "SIGNIFIER" : "INCOMPLETE") + "(\"" + constituent.get().constructString(showBrackets)
						+ "\")");
	}
}
