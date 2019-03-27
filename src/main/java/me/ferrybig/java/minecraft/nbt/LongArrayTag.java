/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;

public class LongArrayTag extends SimpleTag {

	private final long[] value;

	public LongArrayTag(long[] value) {
		super(TagType.LONG_ARRAY);
		this.value = Objects.requireNonNull(value, "value");
	}

	@Nonnull
	@Override
	public long[] get() {
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
		final LongArrayTag other = (LongArrayTag) obj;
		if (!Arrays.equals(this.value, other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "LongArrayTag{" + Arrays.toString(value) + '}';
	}

}
