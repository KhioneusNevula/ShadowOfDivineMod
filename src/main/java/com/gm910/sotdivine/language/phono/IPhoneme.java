package com.gm910.sotdivine.language.phono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.stream.Streams;
import org.apache.logging.log4j.util.Strings;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface IPhoneme {

	public static final String KEY_ID = "id";
	public static final String KEY_FORM = "form";
	public static final String KEY_ID_FORM = "id-form";
	public static final String KEY_FEATURES = "features";
	public static final String KEY_COPY = "copy";

	/**
	 * makes a sequence of phonemes a string
	 * 
	 * @param phonemes
	 * @return
	 */
	public static String toString(Iterable<? extends IPhoneme> phonemes) {
		return Streams.of(phonemes).map(IPhoneme::form).reduce("", Strings::concat);
	}

	public static Optional<String> getCopySource(JsonElement element) {
		return Optional.ofNullable(element.getAsJsonObject().get(KEY_COPY)).map(JsonElement::getAsString);
	}

	public static IPhoneme createPhoneme(Optional<IPhoneme> copyFrom, JsonElement parse) {
		Optional<String> id = Optional.ofNullable(parse.getAsJsonObject().get(KEY_ID))
				.or(() -> Optional.ofNullable(parse.getAsJsonObject().get(KEY_ID_FORM))).map(JsonElement::getAsString);

		Optional<String> form = Optional.ofNullable(parse.getAsJsonObject().get(KEY_FORM))
				.or(() -> Optional.ofNullable(parse.getAsJsonObject().get(KEY_ID_FORM))).map(JsonElement::getAsString);

		P phoneme = new P(id.or(() -> copyFrom.map(IPhoneme::id)).get(),
				form.or(() -> copyFrom.map(IPhoneme::form)).get(),
				copyFrom.map((p) -> new HashMap<>(p.features())).orElse(new HashMap<>()));

		if (parse.getAsJsonObject().get(KEY_FEATURES) instanceof JsonObject features) {
			phoneme.features().putAll(Maps.transformValues(features.asMap(), JsonElement::getAsString));
		}

		return phoneme;
	}

	/**
	 * the id of the phoneme
	 * 
	 * @return
	 */
	public String id();

	/**
	 * the form the phoneme is realized as
	 * 
	 * @return
	 */
	public String form();

	/**
	 * Features as a string map
	 * 
	 * @return
	 */
	public Map<String, String> features();
}
