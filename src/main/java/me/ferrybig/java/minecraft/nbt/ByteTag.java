/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import javax.annotation.Nonnull;

public class ByteTag extends SimpleTag {
	private final byte value;

	public ByteTag(byte value) {
		super(TagType.BYTE);
		this.value = value;
	}

	public byte getValue() {
		return value;
	}

	@Nonnull
	@Override
	public Byte get() {
		return getValue();
	}

	@Override
	public int hashCode() {
		return value | (value << 16);
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
		final ByteTag other = (ByteTag) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ByteTag{" + value + '}';
	}

}
