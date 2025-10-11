package com.gm910.sotdivine.language.element;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.language.feature.ISpecificationValue;

/**
 * Something that makes up a language construction
 */
public interface IConstituent {

	/** the unique id of this element */
	public String id();

	/**
	 * Return the form of this element
	 * 
	 * @return
	 */
	public String form();

	/**
	 * The syntactic category of this element
	 * 
	 * @return
	 */
	public String category();

	/**
	 * The (specified) features of this element
	 * 
	 * @return
	 */
	public Map<String, ISpecificationValue> features();
}
