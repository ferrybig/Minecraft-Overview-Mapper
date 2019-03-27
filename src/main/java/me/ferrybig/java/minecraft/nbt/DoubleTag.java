/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public class DoubleTag extends SimpleTag {

	private final double value;

	public DoubleTag(double value) {
		super(TagType.DOUBLE);
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Double get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		return (int) (Double.doubleToLongBits(this.value) ^ (Double.doubleToLongBits(this.value) >>> 32));
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
		final DoubleTag other = (DoubleTag) obj;
		if (Double.doubleToLongBits(this.value) != Double.doubleToLongBits(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DoubleTag{" + value + '}';
	}

}
