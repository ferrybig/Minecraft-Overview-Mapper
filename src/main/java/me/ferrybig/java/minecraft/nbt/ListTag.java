/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public class ListTag extends Tag implements Iterable<Tag> {

	private final TagType contents;
	private final List<Tag> list;

	public ListTag(TagType contents, List<? extends Tag> list) {
		this(contents, list, true);
	}

	public ListTag(TagType contents, List<? extends Tag> list, boolean copy) {
		super(TagType.LIST);
		this.contents = Objects.requireNonNull(contents, "contents");
		if (copy) {
			List<Tag> newList = new ArrayList<>(list.size());
			for (Tag tag : list) {
				if (tag.getType() != contents) {
					throw new IllegalArgumentException("Passed list contains tag of a differend type: " + tag.getType());
				}
				newList.add(tag);
			}
			this.list = Collections.unmodifiableList(newList);
		} else {
			this.list = Collections.unmodifiableList(list);
		}
	}

	public TagType getContents() {
		return contents;
	}

	@Nonnull
	@Override
	public List<? extends Tag> get() {
		return list;
	}

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<Tag> iterator() {
		return list.iterator();
	}

	@SuppressWarnings("unchecked")
	public Iterable<? extends CompoundTag> compoundTagIterable() {
		if (this.contents != TagType.COMPOUND) {
			throw new IllegalArgumentException("Cannot cast iterator to compound tag iterator");
		}
		return () -> new Iterator<CompoundTag>() {
			Iterator<Tag> iterator = list.iterator();

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public CompoundTag next() {
				return (CompoundTag) iterator.next();
			}
		};
	}

	public Tag get(int index) {
		return list.get(index);
	}

	public ListIterator<? extends Tag> listIterator() {
		return list.listIterator();
	}

	public Stream<? extends Tag> stream() {
		return list.stream();
	}

	@Override
	public void forEach(Consumer<? super Tag> action) {
		list.forEach(action);
	}

	@Override
	protected Resolveable resolveNode(String path) {
		try {
			return get(Integer.parseInt(path));
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			throw new UnsupportedOperationException("A " + this.type + " tag does not have a child called " + path, e);
		}
	}

	public List<Resolveable> resolveList(String... path) {
		List<Resolveable> resolvables = new ArrayList<>(this.size());
		for (Tag t : this.list) {
			resolvables.add(t.resolve(path));
		}
		return resolvables;
	}

	@Override
	public String toString() {
		return "ListTag{" + contents + ": " + list + '}';
	}

}
