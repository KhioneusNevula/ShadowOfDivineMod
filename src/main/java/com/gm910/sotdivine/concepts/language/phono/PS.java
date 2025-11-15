package com.gm910.sotdivine.concepts.language.phono;

import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;

class PS implements IPhonemeSelector {

	private Optional<Integer> so;
	private Optional<String> idorvar;
	private Optional<Map<String, ISpecificationValue>> feats;
	private boolean isvar;
	private boolean opp;

	public PS(boolean isvar, Optional<Integer> so, Optional<String> id,
			Optional<Map<String, ISpecificationValue>> feats, boolean opp) {
		this.so = so;
		this.idorvar = id;
		this.feats = feats;
		this.isvar = isvar;
		this.opp = opp;
	}

	@Override
	public boolean opposite() {
		return opp;
	}

	@Override
	public boolean isVariable() {
		return isvar;
	}

	@Override
	public Optional<Integer> getIdenticalSource() {
		return so;
	}

	@Override
	public Optional<String> getIdOrVar() {
		return idorvar;
	}

	@Override
	public Optional<Map<String, ISpecificationValue>> features() {
		return feats;
	}

	@Override
	public String toString() {
		if (this.isVariable()) {
			return "@" + (opp ? "!" : "") + "<\"" + this.idorvar.get() + "\">";
		} else if (this.so.isPresent()) {
			return "@" + (opp ? "!" : "") + "*(" + this.so.get() + ")";
		} else if (this.feats.isPresent()) {
			return "@" + (opp ? "!" : "") + this.feats.get();
		} else if (this.idorvar.isPresent()) {
			return "@" + (opp ? "!" : "") + "(\"" + this.idorvar.get() + "\")";
		} else {
			throw new IllegalStateException("??so=" + this.so + ",idorvar=" + idorvar + ",opp" + opp + ",isvar="
					+ this.isvar + ",feats=" + feats);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof IPhonemeSelector selec) {
			return this.feats.equals(selec.features()) && this.idorvar.equals(selec.getIdOrVar())
					&& this.so.equals(selec.getIdenticalSource()) && this.isvar == selec.isVariable()
					&& this.opp == selec.opposite();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.feats.hashCode() + this.idorvar.hashCode() + this.so.hashCode();
	}

}
