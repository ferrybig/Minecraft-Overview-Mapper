/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier;

import java.util.Map;
import java.util.Objects;

public class SimpleVariantSpecifier implements VariantSpecifier {

	private final Map<String, String> state;

	public SimpleVariantSpecifier(Map<String, String> state) {
		this.state = state;
	}

	@Override
	public boolean matches(Map<String, String> state) {
		for (Map.Entry<String, String> entry : this.state.entrySet()) {
			if (!Objects.equals(state.get(entry.getKey()), entry.getValue())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 37 * hash + Objects.hashCode(this.state);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SimpleVariantSpecifier other = (SimpleVariantSpecifier) obj;
		if (!Objects.equals(this.state, other.state)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SimpleVariantSpecifier{" + "state=" + state + '}';
	}

	public Map<String, String> getState() {
		return state;
	}

}
