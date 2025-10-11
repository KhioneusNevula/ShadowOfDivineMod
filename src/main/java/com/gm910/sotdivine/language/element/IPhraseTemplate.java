package com.gm910.sotdivine.language.element;

import java.util.Collection;

import com.gm910.sotdivine.language.selector.ITemplateSelector;

/**
 * A language element which can be a template for other elements
 * 
 * @author borah
 *
 */
public interface IPhraseTemplate extends IConstituentTemplate {

	/**
	 * how many selectors are in this phrase
	 * 
	 * @return
	 */
	public int selectorCount();

	/**
	 * Return the variables of this phrase
	 * 
	 * @return
	 */
	public Collection<String> variables();

	/**
	 * return the selectors for the given index
	 * 
	 * @return
	 */
	public Collection<ITemplateSelector> getSelectors(int index);

}
