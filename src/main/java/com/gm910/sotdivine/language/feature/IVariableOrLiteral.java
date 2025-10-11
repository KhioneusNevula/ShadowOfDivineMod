package com.gm910.sotdivine.language.feature;

import java.util.Optional;

public interface IVariableOrLiteral {

	public Optional<String> getVariable();

	/**
	 * If this is a placeholder variable
	 * 
	 * @return
	 */
	public default boolean isVariable() {
		return this.getVariable().isPresent();
	}
}
