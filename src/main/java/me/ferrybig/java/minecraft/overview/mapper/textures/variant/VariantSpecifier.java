/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

public class VariantSpecifier {

	private final SortedMap<String, String> state;

	public VariantSpecifier(SortedMap<String, String> state) {
		this.state = state;
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
		final VariantSpecifier other = (VariantSpecifier) obj;
		if (!Objects.equals(this.state, other.state)) {
			return false;
		}
		return true;
	}

	public SortedMap<String, String> getState() {
		return state;
	}

	public boolean matches(Map<String, String> state) {
		for (Map.Entry<String, String> entry : state.entrySet()) {
			if (!Objects.equals(this.state.get(entry.getKey()), entry.getValue())) {
				return false;
			}
		}
		return true;
	}

}
