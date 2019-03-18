/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;

public class ByteArrayTag extends SimpleTag {

	private final byte[] value;

	public ByteArrayTag(byte[] value) {
		super(TagType.BYTE_ARRAY);
		this.value = Objects.requireNonNull(value, "value");
	}

	@Nonnull
	@Override
	public byte[] get() {
		return value;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.value);
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
		final ByteArrayTag other = (ByteArrayTag) obj;
		if (!Arrays.equals(this.value, other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ByteArrayTag{" + Arrays.toString(value) + '}';
	}

}
