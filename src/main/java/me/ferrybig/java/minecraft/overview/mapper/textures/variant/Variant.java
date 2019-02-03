/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import me.ferrybig.java.minecraft.overview.mapper.textures.blockstate.UnresolvedBlockState;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Stream;
import me.ferrybig.java.minecraft.overview.mapper.textures.Model;

public interface Variant extends Iterable<Model>, UnresolvedBlockState {
	public static Variant EMPTY = new Variant() {
		@Override
		public List<Model> getModels() {
			return Collections.emptyList();
		}

		@Override
		public Iterator<Model> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public Stream<Model> stream() {
			return Stream.empty();
		}

		@Override
		public String toString() {
			return "Variant.EMPTY";
		}
	};

	List<Model> getModels();

	default Stream<Model> stream() {
		return getModels().stream();
	}

	@Override
	public default Variant resolve(Map<String, String> state) {
		return this;
	}

	public static Variant empty() {
		return EMPTY;
	}
}
