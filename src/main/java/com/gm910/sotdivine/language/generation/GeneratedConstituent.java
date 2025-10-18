package com.gm910.sotdivine.language.generation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.stream.Streams;

import com.gm910.sotdivine.language.element.IConstituent;
import com.gm910.sotdivine.language.element.IConstituentTemplate;
import com.gm910.sotdivine.language.element.IConstituentTemplate.ElementType;
import com.gm910.sotdivine.language.element.IPhraseTemplate;
import com.gm910.sotdivine.language.element.IWordTemplate;
import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.google.common.collect.Maps;

/**
 * A constituent of a language construction
 */
public class GeneratedConstituent implements IConstituent {

	/*
	 * public static final Codec<GeneratedConstituent> DEFICIENT_CODEC =
	 * RecordCodecBuilder.create(emanation -> emanation.group(
	 * 
	 * ).apply(emanation, (x, y, z) -> ...));
	 */

	private IConstituentTemplate element;
	private Map<String, String> features;
	private GeneratedConstituent[] arguments;
	// categories to capitalize
	private Set<String> capCats = new HashSet<>();

	public GeneratedConstituent(IConstituentTemplate element) {
		this.element = element;
		if (element instanceof IWordTemplate) {
			this.features = Map.copyOf(Maps.transformValues(element.features(), (x) -> x.getLiteral().get()));
			arguments = new GeneratedConstituent[0];
		} else if (element instanceof IPhraseTemplate phrase) {
			this.features = new HashMap<>(element.features().size());
			phrase.features().entrySet().stream().filter((e) -> e.getValue().isLiteral())
					.forEach((e) -> features.put(e.getKey(), e.getValue().getLiteral().get()));
			arguments = new GeneratedConstituent[phrase.selectorCount()];
		}
	}

	public boolean isRoot() {
		return this.element == null;
	}

	/**
	 * REturn which categories will have their elements capitalized in the output
	 * 
	 * @return
	 */
	public Set<String> getCategoriesToCapitalize() {
		return capCats;
	}

	/**
	 * Return the provider this holder represents
	 * 
	 * @return
	 */
	public IConstituentTemplate template() {
		return element;
	}

	/**
	 * Return the features of this holder; editable. Should all be literals
	 * 
	 * @return
	 */
	public Map<String, String> getFeaturesEditable() {
		return features;
	}

	@Override
	public Map<String, ISpecificationValue> features() {
		return Maps.transformValues(this.features, ISpecificationValue::literal);
	}

	/**
	 * List of arguments this feature holds
	 * 
	 * @return
	 */
	public GeneratedConstituent[] arguments() {
		return arguments;
	}

	/**
	 * Edit internal features based on given variables
	 * 
	 * @param vars
	 */
	public void assignVariables(Map<String, ISpecificationValue> vars) {
		this.element.features().entrySet().stream().filter((x) -> x.getValue().isVariable()).forEach((en) -> {
			String var = en.getValue().getVariable().get();
			if (vars.containsKey(var) && vars.get(var).isLiteral()) {
				this.features.put(en.getKey(), vars.get(var).getLiteral().get());
			}
		});
	}

	private String constructString(boolean showBrackets, Collection<String> capCats) {
		if (this.isRoot()) {
			return Optional.ofNullable(this.arguments[0]).map((x) -> x.constructString(showBrackets))
					.orElse("<empty root>");
		}
		String template = element.form();
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i] != null) {
				template = template.replace("{" + i + "}", arguments[i].constructString(showBrackets, capCats));
			}
		}
		if (capCats.contains(element.category())) {
			template = (template.charAt(0) + "").toUpperCase() + template.substring(1);
		}
		return (showBrackets ? "[_" + element.category() + " " : "") + template + (showBrackets ? "]" : "");
	}

	/**
	 * Create the full string of these elements
	 * 
	 * @param showBrackets whether to show brackets around recursive constructions
	 * @return
	 */
	public String constructString(boolean showBrackets) {
		return constructString(showBrackets, this.capCats);
	}

	@Override
	public String toString() {
		return "GeneratedConstituent{id=" + Optional.ofNullable(element).map(IConstituentTemplate::id).orElse("<root>")
				+ ",features=" + this.features + ",nested=" + this.constructString(true) + "}";
	}

	@Override
	public int hashCode() {
		return Optional.ofNullable(this.element).map(IConstituentTemplate::id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof GeneratedConstituent holder) {
			return Optional.ofNullable(this.element).equals(Optional.ofNullable(holder.template()))
					&& this.features.equals(holder.features) && Arrays.equals(this.arguments, holder.arguments);
		}

		return false;
	}

	@Override
	public String id() {
		return Optional.ofNullable(this.element).map(IConstituentTemplate::id).orElse("<root>");
	}

	@Override
	public String form() {
		return Optional.ofNullable(this.element).map(IConstituentTemplate::form).orElse("<empty>");
	}

	@Override
	public String category() {
		return Optional.ofNullable(this.element).map(IConstituentTemplate::category).orElse("");
	}

	public int words() {
		return element.elementType() == ElementType.WORD ? 1
				: Streams.of(this.arguments).mapToInt((g) -> g == null ? 1 : g.words()).sum();
	}

}
