package com.gm910.sotdivine.concepts.language.selector;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gm910.sotdivine.concepts.language.element.IConstituentTemplate;
import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;

/**
 * A selector for a language provider
 * 
 * @author borah
 *
 */
public interface ITemplateSelector extends ILSelector {

	public static ITemplateSelector createNew() {
		return new TS();
	}

	/**
	 * Create a language selector
	 * 
	 * @param categories
	 * @param features
	 * @return
	 */
	public static ITemplateSelector create(Collection<String> categories,
			Multimap<String, ISpecificationValue> features) {
		return new TS().addCategories(categories).addFeatures(features);
	}

	/**
	 * Return a copy with the features replaced for the given features
	 * 
	 * @param feature
	 * @param value
	 * @return
	 */
	public default ITemplateSelector withFeatures(Multimap<String, ISpecificationValue> value) {
		TS sel = new TS();
		sel.copy(this);
		for (String key : value.keySet()) {
			sel.featuresMap.removeAll(key);
		}
		sel.featuresMap.putAll(value);
		return sel;

	}

	@Override
	public default ITemplateSelector withUpdatedVariables(Multimap<String, ISpecificationValue> variables) {
		TS sel = new TS();
		sel.copy(this);
		for (Entry<String, Collection<ISpecificationValue>> entry : new HashSet<>(sel.featuresMap.asMap().entrySet())) {
			for (ISpecificationValue spec : new HashSet<>(entry.getValue())) {
				if (spec.getVariable().filter(variables::containsKey).isPresent()) {
					variables.get(spec.getVariable().get()).forEach((var) -> entry.getValue().add(var));
					entry.getValue().remove(spec);
				}
			}
		}
		return sel;

	}

	@Override
	default ILSelector withUpdatedSemanticVariables(Map<String, ISemanticSpecificationValue> variables) {
		TS sel = new TS();
		sel.copy(this);
		List.of(sel.semanticsMap, sel.aSemanticsMap).forEach((semap) -> {
			semap.entrySet().stream().filter((en) -> en.getValue().getVariable().map(variables::get).isPresent())
					.forEach((en) -> en.setValue(en.getValue().getVariable().map(variables::get).get()));
		});
		return sel;
	}

	/**
	 * Updates variables for this selector based on the selected template and
	 * selector that selected it
	 * 
	 * @param templateSelector
	 * @return
	 */
	public default ITemplateSelector withUpdatedVariables(IConstituentTemplate template, ILSelector templateSelector) {
		TS sel = (TS) new TS().copy(this);

		Multimap<String, ISpecificationValue> variableMap = MultimapBuilder.hashKeys().hashSetValues().build();
		for (String feature : template.features().keySet()) {
			ISpecificationValue templateSpec = template.features().get(feature);
			if (!templateSpec.isVariable())
				continue;
			for (ISpecificationValue spec : templateSelector.features().get(feature)) {
				// if (!spec.isLiteral())
				// continue;
				variableMap.put(templateSpec.getString(), spec);
			}
		}
		return sel.withUpdatedVariables(variableMap);
	}

	/**
	 * Create a copy ofthis selector with given adjunct semantics
	 * 
	 * @param sem
	 * @return
	 */
	public ITemplateSelector withAdjunctSemantics(Map<String, ISemanticSpecificationValue> sem);

	/**
	 * Create a copy ofthis selector with given head semantics
	 * 
	 * @param sem
	 * @return
	 */
	public ITemplateSelector withHeadSemantics(Map<String, ISemanticSpecificationValue> sem);

	/**
	 * Copy this language selector from another selector
	 * 
	 * @param other
	 */
	public ITemplateSelector copy(ITemplateSelector other);

	/**
	 * Parse a language selector
	 * 
	 * @param fromEl
	 * @return
	 */
	public ITemplateSelector parse(JsonElement fromEl);

	/**
	 * Whether this is selecting for the head of a phrase
	 * 
	 * @return
	 */
	public boolean isHead();

}
