/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import java.util.Objects;
import javax.annotation.Nonnull;

public class StringTag extends SimpleTag {
	private final String value;

	public StringTag(String value) {
		super(TagType.STRING);
		this.value = Objects.requireNonNull(value, "value");
	}

	@Nonnull
	@Override
	public String get() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.value);
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
		final StringTag other = (StringTag) obj;
		if (!Objects.equals(this.value, other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "StringTag{" + value + '}';
	}

}
