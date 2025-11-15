package com.gm910.sotdivine.concepts.language.element;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue;

/**
 * An atomic provider of a language
 * 
 * @author borah
 *
 */
public interface IWordTemplate extends IConstituentTemplate {

	/**
	 * Return sematnic categories of this word
	 * 
	 * @return
	 */
	public Map<String, ISemanticSpecificationValue> semantics();

	/**
	 * The root this word was derived from
	 * 
	 * @return
	 */
	public Optional<IWordTemplate> root();
}
