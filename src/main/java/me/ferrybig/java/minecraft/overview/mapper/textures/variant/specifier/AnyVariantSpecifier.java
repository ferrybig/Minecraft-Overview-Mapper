/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class AnyVariantSpecifier implements VariantSpecifier {

	private final String key;
	private final String[] values;

	public AnyVariantSpecifier(String key, String[] values) {
		this.key = key;
		this.values = values;
	}

	@Override
	public boolean matches(Map<String, String> state) {
		String value = state.get(key);
		if (value == null) {
			return false;
		}
		for (String target : values) {
			if (!value.equals(target)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + Objects.hashCode(this.key);
		hash = 53 * hash + Arrays.deepHashCode(this.values);
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
		final AnyVariantSpecifier other = (AnyVariantSpecifier) obj;
		if (!Objects.equals(this.key, other.key)) {
			return false;
		}
		if (!Arrays.deepEquals(this.values, other.values)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AnyVariantSpecifier{" + "key=" + key + ", values=" + Arrays.toString(values) + '}';
	}

}
