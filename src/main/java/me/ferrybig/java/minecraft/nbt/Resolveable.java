/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import java.util.Arrays;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Resolveable {
	public abstract Tag asTag();

	@Nonnull
	public ByteTag asByteTag() {
		return (ByteTag) asTag();
	}

	@Nonnull
	public ShortTag asShortTag() {
		return (ShortTag) asTag();
	}

	@Nonnull
	public FloatTag asFloatTag() {
		return (FloatTag) asTag();
	}

	@Nonnull
	public IntTag asIntTag() {
		return (IntTag) asTag();
	}

	@Nonnull
	public LongTag asLongTag() {
		return (LongTag) asTag();
	}

	@Nonnull
	public DoubleTag asDoubleTag() {
		return (DoubleTag) asTag();
	}

	@Nonnull
	public StringTag asStringTag() {
		return (StringTag) asTag();
	}

	@Nonnull
	public ListTag asListTag() {
		return (ListTag) asTag();
	}

	@Nonnull
	public CompoundTag asCompoundTag() {
		return (CompoundTag) asTag();
	}

	@Nonnull
	public ByteArrayTag asByteArrayTag() {
		return (ByteArrayTag) asTag();
	}

	@Nonnull
	public IntArrayTag asIntArrayTag() {
		return (IntArrayTag) asTag();
	}

	@Nonnull
	public LongArrayTag asLongArrayTag() {
		return (LongArrayTag) asTag();
	}

	@Nonnull
	public Resolveable resolve(String... path) {
		Resolveable node = this;
		for(int i = 0; i < path.length; i++) {
			try {
				node = node.resolveNode(path[i]);
			} catch(UnsupportedOperationException e) {
				throw (NoSuchElementException)new NoSuchElementException("Could not find path to " + Arrays.toString(Arrays.copyOf(path, i))).initCause(e);
			}
			if (node == null) {
				throw new NoSuchElementException("Could not find path to " + Arrays.toString(Arrays.copyOf(path, i)));
			}
		}
		return node;
	}

	@Nullable
	public Resolveable resolveOrNull(String... path) {
		Resolveable node = this;
		for(int i = 0; i < path.length; i++) {
			node = node.resolveNode(path[i]);
			if(node == null) {
				return null;
			}
		}
		return node;
	}

	@Nullable
	protected abstract Resolveable resolveNode(String path);
}
