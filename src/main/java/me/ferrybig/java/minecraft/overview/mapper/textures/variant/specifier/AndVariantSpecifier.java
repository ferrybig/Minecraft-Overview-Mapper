/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier;

import java.util.Arrays;
import java.util.Map;

public class AndVariantSpecifier implements VariantSpecifier {

	private final VariantSpecifier[] specifiers;

	public AndVariantSpecifier(VariantSpecifier[] specifiers) {
		this.specifiers = specifiers;
	}

	@Override
	public boolean matches(Map<String, String> state) {
		for (VariantSpecifier specifier : specifiers) {
			if (!specifier.matches(state)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79 * hash + Arrays.deepHashCode(this.specifiers);
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
		final AndVariantSpecifier other = (AndVariantSpecifier) obj;
		if (!Arrays.deepEquals(this.specifiers, other.specifiers)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "CombinedVariantSpecifier{" + "specifiers=" + specifiers + '}';
	}

}
