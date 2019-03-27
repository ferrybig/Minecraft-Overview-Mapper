/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public final class ShortTag extends SimpleTag {

	private final short value;

	public ShortTag(short value) {
		super(TagType.SHORT);
		this.value = value;
	}

	public short getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Short get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		return (this.value ^ 0b01110101) | (this.value >> 16);
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
		final ShortTag other = (ShortTag) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ShortTag{" + value + '}';
	}

}
