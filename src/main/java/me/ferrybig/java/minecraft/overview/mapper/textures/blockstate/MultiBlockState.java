/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.CombinedVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;

public class MultiBlockState implements UnresolvedBlockState {

	private final List<Entry> partList;

	public MultiBlockState(Map<VariantSpecifier, Variant> list) {
		this.partList = list.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public Variant resolve(Map<String, String> state) {
		List<Variant> matched = this.partList.stream()
			.filter(e -> e.specifier.matches(state))
			.map(Entry::getVariant)
			.collect(Collectors.toList());

		int size = matched.size();
		switch(size) {
			case 0:
				return Variant.empty();
			case 1:
				return matched.get(0);
			default:
				return new CombinedVariant(matched);
		}
	}

	private static class Entry {
		private final VariantSpecifier specifier;
		private final Variant variant;

		public Entry(VariantSpecifier specifier, Variant variant) {
			this.specifier = specifier;
			this.variant = variant;
		}

		public VariantSpecifier getSpecifier() {
			return specifier;
		}

		public Variant getVariant() {
			return variant;
		}

	}

}
