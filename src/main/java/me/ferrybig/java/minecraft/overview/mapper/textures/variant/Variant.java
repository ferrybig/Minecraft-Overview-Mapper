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
import java.util.stream.Stream;

public interface Variant extends Iterable<VariantModel>, UnresolvedBlockState {

	public static Variant EMPTY = new Variant() {
		@Override
		public List<VariantModel> getModels() {
			return Collections.emptyList();
		}

		@Override
		public Iterator<VariantModel> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public Stream<VariantModel> stream() {
			return Stream.empty();
		}

		@Override
		public String toString() {
			return "Variant.EMPTY";
		}
	};

	List<VariantModel> getModels();

	default Stream<VariantModel> stream() {
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
