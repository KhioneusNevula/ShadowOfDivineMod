package com.gm910.sotdivine.concepts.language.feature;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.language.element.IConstituent;
import com.gm910.sotdivine.concepts.language.element.IConstituentTemplate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * A feature-specification of the language
 * 
 * @author borah
 *
 */
public interface ISpecificationValue extends IVariableOrLiteral {

	public static final String KEY_OPPOSITE = "opposite";
	public static final String KEY_VARIABLE = "var";
	public static final String KEY_LITERAL = "value";

	public static ISpecificationValue literal(String phrase) {
		return literal(phrase, false);
	}

	public static ISpecificationValue literal(String phrase, boolean opposite) {
		return new SV(phrase, SpecificationType.LITERAL, false);
	}

	public static ISpecificationValue var(String phrase, boolean opposite) {
		return new SV(phrase, SpecificationType.VARIABLE, false);
	}

	public static ISpecificationValue var(String phrase) {
		return var(phrase, false);
	}

	public static ISpecificationValue bool(boolean value) {
		return new SV(value + "", SpecificationType.LITERAL, false);
	}

	/**
	 * Specificate the value of a variable in this specification; return a literal
	 * with the new variable value. Error if this is not a variable specification
	 * 
	 * @param var
	 * @param val
	 * @return
	 */
	public ISpecificationValue specifyVariable(String val);

	public static ISpecificationValue parse(JsonElement element) {
		try {
			if (element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				boolean opposite = Optional.ofNullable(object.get(KEY_OPPOSITE)).map(JsonElement::getAsBoolean)
						.orElse(false);
				String item;
				SpecificationType type;
				if (object.has(KEY_VARIABLE)) {
					type = SpecificationType.VARIABLE;
					item = object.get(KEY_VARIABLE).getAsString();
				} else if (object.has(KEY_LITERAL)) {
					type = SpecificationType.LITERAL;
					item = object.get(KEY_LITERAL).getAsString();
				} else {
					type = SpecificationType.LITERAL;
					item = !opposite + "";
					opposite = false;
				}

				return new SV(item, type, opposite);

			} else {
				String literal = element.getAsString();
				SpecificationType type = SpecificationType.LITERAL;
				boolean opp = false;
				return new SV(literal, type, opp);
			}
		} catch (Exception e) {
			throw new JsonParseException("While parsing specification encoded in " + element + ": " + e.getMessage(),
					e);
		}
	}

	/**
	 * Whether this is a feature Literal or a feature Variable
	 * 
	 * @return
	 */
	public SpecificationType type();

	/**
	 * Whether this specification is a single boolean value, with no actual literal
	 * value
	 * 
	 * @return
	 */
	public boolean isBoolean();

	public default boolean isLiteral() {
		return this.type() == SpecificationType.LITERAL;
	}

	public default boolean isVariable() {
		return this.type() == SpecificationType.VARIABLE;
	}

	/**
	 * Whether this feature is marked as "opposite"
	 * 
	 * @return
	 */
	public boolean opposite();

	/**
	 * Gets the literal of this feature, if this is of SpecificationType
	 * {@link SpecificationType#LITERAL}
	 * 
	 * @return
	 */
	public Optional<String> getLiteral();

	/**
	 * Gets the variable of this feature, if this is of type
	 * {@link SpecificationType#VARIABLE}
	 * 
	 * @return
	 */
	public Optional<String> getVariable();

	/**
	 * Return the string, whether variable or literal
	 * 
	 * @return
	 */
	public String getString();

}
