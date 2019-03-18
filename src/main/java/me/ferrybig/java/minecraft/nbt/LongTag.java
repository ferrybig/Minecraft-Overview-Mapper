/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public class LongTag extends SimpleTag {
	private final long value;

	public LongTag(long value) {
		super(TagType.LONG);
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Long get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		return (int) (this.value ^ (this.value >>> 32));
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
		final LongTag other = (LongTag) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "LongTag{"+ value + '}';
	}

}
