/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public class IntTag extends SimpleTag {

	private final int value;

	public IntTag(int value) {
		super(TagType.INT);
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Integer get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		return this.value;
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
		final IntTag other = (IntTag) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "IntTag{" + value + '}';
	}

}
