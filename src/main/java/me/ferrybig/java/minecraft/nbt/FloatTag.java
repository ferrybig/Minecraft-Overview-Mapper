/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public class FloatTag extends SimpleTag {
	private final float value;

	public FloatTag(float value) {
		super(TagType.FLOAT);
		this.value = value;
	}

	public float getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Float get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 73 * hash + Float.floatToIntBits(this.value);
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
		final FloatTag other = (FloatTag) obj;
		if (Float.floatToIntBits(this.value) != Float.floatToIntBits(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "FloatTag{" + value + '}';
	}

}
