/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public class CompoundTag extends Tag {

	private final Map<String, ? extends Tag> map;

	public CompoundTag(Map<String, ? extends Tag> map) {
		this(map, true);
	}
	public CompoundTag(Map<String, ? extends Tag> map, boolean copy) {
		super(TagType.COMPOUND);
		if (copy) {
			this.map = new HashMap<>(map);
		} else {
			this.map = map;
		}
	}

	@Nonnull
	@Override
	public Map<String, ? extends Tag> get() {
		return map;
	}

	@Override
	protected Resolveable resolveNode(String path) {
		Tag tag = this.map.get(path);
		return tag;
	}

	@Override
	public String toString() {
		return "CompoundTag{" + map + '}';
	}

}
