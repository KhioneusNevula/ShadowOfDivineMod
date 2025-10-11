package com.gm910.sotdivine.language.phono;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * A single phonotactic rule
 */
public interface IPhonotacticDisallow {

	public static final String KEY_DISALLOWED = "disallowed";
	public static final String KEY_VARIABLES = "variables";

	public static IPhonotacticDisallow parse(JsonElement json) {
		JsonObject variables = null;
		if (json instanceof JsonObject object) {
			json = object.get(KEY_DISALLOWED);
			if (object.get(KEY_VARIABLES) instanceof JsonObject vava) {
				variables = vava;
			}
		}

		List<IPhonemeSelector> sequence;
		Multimap<String, IPhonemeSelector> varMulti = MultimapBuilder.hashKeys().hashSetValues().build();

		if (json instanceof JsonArray array) {
			try {
				sequence = new ArrayList<>(array.size());
				for (JsonElement pselec : array) {
					int copya = IPhonemeSelector.copyIndex(pselec);
					IPhonemeSelector copySource = copya >= 0 ? sequence.get(copya) : null;
					sequence.add(IPhonemeSelector.fromJson(copySource, pselec));
				}

			} catch (Exception e) {
				throw new JsonParseException("While parsing " + json, e);
			}
		} else {
			throw new JsonParseException("Cannot parse whatever this is...");
		}
		if (variables != null) {
			for (String varKey : variables.keySet()) {
				variables.get(varKey).getAsJsonArray().forEach((el) -> {
					int copya = IPhonemeSelector.copyIndex(el);
					IPhonemeSelector copySource = copya >= 0 ? sequence.get(copya) : null;
					varMulti.put(varKey, IPhonemeSelector.fromJson(copySource, el));
				});
			}
		}

		return new PT(sequence, varMulti);
	}

	/**
	 * the selectors for the prefix
	 * 
	 * @return
	 */
	public List<IPhonemeSelector> getPrefix();

	/**
	 * The disallowed phoneme
	 * 
	 * @return
	 */
	public IPhonemeSelector disallowedPhoneme();

	/**
	 * Provide variables
	 * 
	 * @return
	 */
	public Multimap<String, IPhonemeSelector> variables();

	/**
	 * Test whether the given phoneme can come next. Put null for the given phoneme
	 * if it is the End.
	 * 
	 * @param sequence
	 * @param beginning whether this sequence starts from the beginning
	 * @return
	 */
	public boolean matchPattern(List<IPhoneme> sequence, IPhoneme phoneme);

	/**
	 * Whether this phonotactic starts at word-beginining
	 * 
	 * @return
	 */
	boolean isAtBegin();

	/**
	 * Whether this phonotactic ends word-finally
	 * 
	 * @return
	 */
	boolean isAtEnd();
}
