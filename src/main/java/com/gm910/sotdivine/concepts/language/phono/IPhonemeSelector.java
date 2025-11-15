package com.gm910.sotdivine.concepts.language.phono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A selector of phonemes
 */
public interface IPhonemeSelector {

	public static final String KEY_IDENTICAL = "$identical";
	public static final String KEY_COPY = "$copy";
	public static final String KEY_VARIABLE = "$var";
	public static final String KEY_OPPOSITE = "$opposite";
	public static final String KEY_ID = "$id";

	/**
	 * Return what index to copy from, or -1 if not copying
	 * 
	 * @return
	 */
	public static int copyIndex(JsonElement json) {
		return Optional.ofNullable(json).filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
				.map((js) -> js.get(KEY_COPY)).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsJsonPrimitive)
				.filter(JsonPrimitive::isNumber).map(JsonPrimitive::getAsInt).orElse(-1);
	}

	public static IPhonemeSelector fromJson(@Nullable IPhonemeSelector copyFrom, JsonElement json) {
		if (json.isJsonPrimitive()) {
			if (json.getAsJsonPrimitive().isString()) {
				return new PS(false, Optional.empty(), Optional.of(json.getAsString()), Optional.empty(), false);
			}
		}

		Map<String, ISpecificationValue> features = new HashMap<>(
				copyFrom != null && copyFrom.features().isPresent() ? copyFrom.features().get() : Map.of());

		boolean opp = Optional.ofNullable(json.getAsJsonObject().get(KEY_OPPOSITE)).map(JsonElement::getAsBoolean)
				.orElse(copyFrom != null ? copyFrom.opposite() : false);
		if (copyFrom != null) {
			if (copyFrom.getIdenticalSource().isPresent()) {
				return new PS(false, copyFrom.getIdenticalSource(), Optional.empty(), Optional.empty(), opp);
			} else if (copyFrom.getIdOrVar().isPresent()) {
				return new PS(copyFrom.isVariable(), Optional.empty(), copyFrom.getIdOrVar(), Optional.empty(), opp);
			}
		}
		for (String key : json.getAsJsonObject().keySet()) {
			JsonElement value = json.getAsJsonObject().get(key);
			if (key.equals(KEY_COPY))
				continue;
			if (key.equals(KEY_IDENTICAL)) {
				return new PS(false, Optional.of(value.getAsInt()), Optional.empty(), Optional.empty(), opp);
			} else if (key.equals(KEY_VARIABLE)) {
				return new PS(true, Optional.empty(), Optional.of(value.getAsString()), Optional.empty(), opp);
			} else if (key.equals(KEY_ID)) {
				return new PS(false, Optional.empty(), Optional.of(value.getAsString()), Optional.empty(), opp);
			}
			features.put(key, ISpecificationValue.parse(value));
		}
		return new PS(false, Optional.empty(), Optional.empty(), Optional.ofNullable(features), opp);
	}

	/**
	 * create an identical selector
	 * 
	 * @param source
	 * @return
	 */
	public static IPhonemeSelector createIdentical(int source) {
		return new PS(false, Optional.of(source), Optional.empty(), Optional.empty(), false);
	}

	/**
	 * If this is just an identical phoneme, get the source that it is being derived
	 * from
	 * 
	 * @return
	 */
	public Optional<Integer> getIdenticalSource();

	/**
	 * If this is a variable
	 * 
	 * @return
	 */
	public boolean isVariable();

	/**
	 * If this is opposite
	 * 
	 * @return
	 */
	public boolean opposite();

	/**
	 * If this selector selects for id, return that; or return the variable this
	 * selector is
	 * 
	 * @return
	 */
	public Optional<String> getIdOrVar();

	/**
	 * If this selector selects for features, return that
	 * 
	 * @return
	 */
	public Optional<Map<String, ISpecificationValue>> features();

}
