package com.gm910.sotdivine.concepts.language.element;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;

/**
 * Something that makes up a language construction
 */
public interface IConstituent {

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
	 * The (specified) features of this provider
	 * 
	 * @return
	 */
	public Map<String, ISpecificationValue> features();
}
