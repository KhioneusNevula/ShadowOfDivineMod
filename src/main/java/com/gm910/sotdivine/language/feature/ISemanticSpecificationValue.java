package com.gm910.sotdivine.language.feature;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;

/**
 * How to construct a semantic specification: if just a boolean, i.e. "true",
 * then the result will be "{specified:true}" Two options: {
 * 
 * "variable":(name) --> indicates that the full specification will be given by
 * a variable
 * 
 * }
 * 
 * otherwise
 * 
 * {
 * 
 * "specified":true/false --> whether the value has to be specified
 * 
 * "disallowed":(list) --> what values are disallowed for this feature
 * 
 * "allowed":(list) --> what values are allowed for this feature (but don't all
 * need to be present)
 * 
 * "required":(list) --> what values are obligatory for this feature (and do
 * need to all be present)
 * 
 * }
 */
public interface ISemanticSpecificationValue extends IVariableOrLiteral {

	public static final String KEY_SPECIFIED = "specified";
	public static final String KEY_ALLOWED = "allowed";
	public static final String KEY_VALUES = "values";
	public static final String KEY_DISALLOWED = "disallowed";
	public static final String KEY_REQUIRED = "required";
	public static final String KEY_VARIABLE = "variable";

	public static enum SemanticConstraint {
		/** Disjunct allowance of values */
		ALLOWED(KEY_ALLOWED),
		/** Disallowance of any values */
		DISALLOWED(KEY_DISALLOWED),
		/** Conjunct allowance of values */
		REQUIRED(KEY_REQUIRED),
		/**
		 * For non-selective specifications, this indicates the values that are present
		 * in and of themselves
		 */
		VALUE(KEY_VALUES);

		public final String key;

		private SemanticConstraint(String key) {
			this.key = key;
		}
	}

	/**
	 * Only for parsing json later
	 * 
	 * @return
	 */
	public static ISemanticSpecificationValue empty() {
		return new SSV();
	}

	/**
	 * A non-selective value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue value(Iterable<String> values) {
		SSV ssv = new SSV();
		ssv.conditions.get().putAll(SemanticConstraint.VALUE, values);
		return ssv;
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specification(Multimap<SemanticConstraint, String> values) {
		SSV ssv = new SSV();
		ssv.conditions.get().putAll(values);
		return ssv;
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specification(
			Map<SemanticConstraint, ? extends Iterable<String>> values) {
		SSV ssv = new SSV();
		for (SemanticConstraint constraint : values.keySet()) {
			ssv.conditions.get().putAll(constraint, values.get(constraint));
		}
		return ssv;
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specificationRequirement(Iterable<String> values) {
		return specification(SemanticConstraint.REQUIRED, values);
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specificationAllow(Iterable<String> values) {
		return specification(SemanticConstraint.ALLOWED, values);
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specificationDisallow(Iterable<String> values) {
		return specification(SemanticConstraint.DISALLOWED, values);
	}

	/**
	 * A selectional value
	 * 
	 * @param values
	 * @return
	 */
	public static ISemanticSpecificationValue specification(SemanticConstraint constraint, Iterable<String> values) {
		SSV ssv = new SSV();
		ssv.conditions.get().putAll(constraint, values);
		return ssv;
	}

	/**
	 * the minimum specification, i.e. that the semantic feature must be specified
	 * at all
	 * 
	 * @return
	 */
	public static ISemanticSpecificationValue minimumSpecification() {
		return SSV.minSpec;
	}

	/**
	 * A variable ssv
	 * 
	 * @param name
	 * @return
	 */
	public static ISemanticSpecificationValue variable(String name) {
		SSV ssv = new SSV();
		ssv.conditions = Optional.empty();
		ssv.variable = Optional.of(name);
		return ssv;
	}

	public boolean mustBeSpecified();

	public Optional<Collection<String>> getConstraint(SemanticConstraint constraint);

	public Optional<String> getVariable();

	/**
	 * If this is a placeholder variable
	 * 
	 * @return
	 */
	public default boolean isVariable() {
		return this.getVariable().isPresent();
	}

	/**
	 * Technically the opposite of {@link #isVariable()}
	 * 
	 * @return
	 */
	public default boolean isSpecification() {
		return this.getVariable().isEmpty();
	}

	/**
	 * Whether this does not act as a selector
	 * 
	 * @return
	 */
	public default boolean isNonSelective() {
		return this.getConstraint(SemanticConstraint.VALUE).filter((p) -> !p.isEmpty()).isPresent();
	}

	/**
	 * Whether this acts as a selector or not
	 * 
	 * @return
	 */
	public default boolean isSelective() {
		return !isNonSelective();
	}

	/**
	 * return a copy with these values removed
	 * 
	 * @param constraint
	 * @param values
	 * @return
	 */
	public ISemanticSpecificationValue withRemoved(SemanticConstraint constraint, Iterable<String> values);

	/**
	 * return a copy with these values added
	 * 
	 * @param constraint
	 * @param values
	 * @return
	 */
	public ISemanticSpecificationValue withAdded(SemanticConstraint constraint, Iterable<String> values);

	/**
	 * Add contents of this to this
	 * 
	 * @param value
	 * @return
	 */
	public ISemanticSpecificationValue withAdded(ISemanticSpecificationValue value);

	public ISemanticSpecificationValue copy(ISemanticSpecificationValue other);

	public ISemanticSpecificationValue parse(JsonElement element, boolean asSelector);

}
