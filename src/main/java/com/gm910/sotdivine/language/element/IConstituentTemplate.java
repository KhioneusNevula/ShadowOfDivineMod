package com.gm910.sotdivine.language.element;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.language.element.IConstituentTemplate.ElementType;
import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.google.gson.JsonElement;

/**
 * A specific item in the language
 * 
 * @author borah
 *
 */
public interface IConstituentTemplate extends IConstituent {

	/**
	 * Prefix indicating someting is being removed
	 */
	public static final String PREFIX_REMOVE = "remove-";

	public static final String KEY_COPY = "copy";
	public static final String KEY_IS_HEAD = "is-head";
	public static final String KEY_CATEGORY = "category";
	public static final String KEY_CAPITALIZE = "capitalize";
	public static final String KEY_FEATURES = "features";
	public static final String KEY_FEATURES_COPY = "copy-features";
	public static final String KEY_FORM = "form";
	public static final String KEY_ID = "id";
	public static final String KEY_VARIABLES = "variables";
	public static final String KEY_SELECTORS = "selectors";
	public static final String KEY_SEMANTICS = "semantics";
	public static final String KEY_ADJUNCT_SEMANTICS = "adjunct-semantics";
	public static final String KEY_DERIVATIONS = "derivations";

	public static enum ElementType {
		WORD, PHRASE
	}

	/**
	 * Create a language provider by type
	 * 
	 * @param type
	 * @return
	 */
	public static IConstituentTemplate create(ElementType type) {
		switch (type) {
		case PHRASE:
			return new PLE();
		case WORD:
			return new WLE();
		}
		return null;
	}

	public static IWordTemplate createWord() {
		return (IWordTemplate) create(ElementType.WORD);
	}

	public static IPhraseTemplate createPhrase() {
		return (IPhraseTemplate) create(ElementType.PHRASE);
	}

	/**
	 * Whether this is a word or phrase
	 * 
	 * @return
	 */
	public default ElementType elementType() {
		return this instanceof IWordTemplate ? ElementType.WORD : ElementType.PHRASE;
	}

	/** the unique id of this provider */
	public String id();

	/**
	 * Return the form of this provider
	 * 
	 * @return
	 */
	public String form();

	/**
	 * The syntactic category of this provider
	 * 
	 * @return
	 */
	public String category();

	/**
	 * To string with more details
	 * 
	 * @return
	 */
	public String detailedString();

	/**
	 * The (specified) features of this provider, if available
	 * 
	 * @return
	 */
	public Map<String, ISpecificationValue> features();

	/**
	 * Return the (optional) id of the provider to be copied from the given provider
	 * definition
	 * 
	 * @return
	 */
	public static Optional<String> getCopySource(JsonElement item) {
		return Optional.ofNullable(item.getAsJsonObject().get(KEY_COPY)).map(JsonElement::getAsString);

	}

	/**
	 * Try to return an array of the derivations of this provider, or an empty one if
	 * not possible
	 * 
	 * @param item
	 * @return
	 */
	public Collection<IConstituentTemplate> genDerivations(JsonElement element, ElementType type);

	/**
	 * copy another language provider
	 * 
	 * @param other
	 */
	public IConstituentTemplate copy(IConstituentTemplate other);

	/**
	 * Parse data into this language provider
	 * 
	 * @param provider
	 */
	public IConstituentTemplate parse(JsonElement element);

}
