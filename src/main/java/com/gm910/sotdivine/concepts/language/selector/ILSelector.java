package com.gm910.sotdivine.concepts.language.selector;

import java.util.Collection;
import java.util.Map;

import com.gm910.sotdivine.concepts.language.element.IConstituentTemplate;
import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;
import com.gm910.sotdivine.concepts.language.generation.GeneratedConstituent;
import com.google.common.collect.Multimap;

/**
 * A selector to test an {@link IConstituentTemplate} based on a given variable
 * environment
 */
public interface ILSelector {

	/**
	 * Returns a map of variables that this selector has specified
	 * 
	 * @param provider
	 * @return
	 */
	public Map<String, ISpecificationValue> obtainSetVariables(GeneratedConstituent element);

	/**
	 * Return true if this matches and has matching specifications for all the
	 * features and semantics that this selector selectors for. In the given
	 * provider, variables will not be evaluated, and features with no specification
	 * will be ignored. Variables in this selector will also not be evaluated if
	 * nothing is provided for them in the environment.
	 * 
	 * If any literal features/semantics could not get matched, or the category
	 * failed to instrument, return false
	 * 
	 * @param provider
	 * @return
	 */
	public boolean test(IConstituentTemplate element);

	/**
	 * The semantic expectations of this provider's adjuncts; copied over from
	 * prvious selector
	 * 
	 * @return
	 */
	Map<String, ISemanticSpecificationValue> adjunctSemantics();

	/**
	 * The possible syntactic categories of this provider
	 * 
	 * @return
	 */
	Collection<String> category();

	/**
	 * The semantic expectations of this provider's head
	 * 
	 * @return
	 */
	public Map<String, ISemanticSpecificationValue> semantics();

	/**
	 * Return a copy with variable values replaced
	 * 
	 * @param feature
	 * @param value
	 * @return
	 */
	ILSelector withUpdatedVariables(Multimap<String, ISpecificationValue> variables);

	/**
	 * Return a copy with semantic variable values replaced
	 * 
	 * @param feature
	 * @param value
	 * @return
	 */
	ILSelector withUpdatedSemanticVariables(Map<String, ISemanticSpecificationValue> variables);

	/**
	 * The (specified) features of this provider
	 * 
	 * @return
	 */
	Multimap<String, ISpecificationValue> features();

}
