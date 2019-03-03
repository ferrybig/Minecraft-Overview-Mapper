/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.VariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.SimpleVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;

public class MultiBlockState implements UnresolvedBlockState {

	private final List<Entry> partList;

	public MultiBlockState(Map<VariantSpecifier, VariantModel> list) {
		this.partList = list.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	public MultiBlockState(List<Map.Entry<VariantSpecifier, VariantModel>> list) {
		this.partList = list.stream().map(e -> new Entry(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public Variant resolve(Map<String, String> state) {
		List<VariantModel> matched = this.partList.stream()
			.filter(e -> e.specifier.matches(state))
			.map(Entry::getVariant)
			.collect(Collectors.toList());

		return new SimpleVariant(matched);
	}

	private static class Entry {

		private final VariantSpecifier specifier;
		private final VariantModel variant;

		public Entry(VariantSpecifier specifier, VariantModel variant) {
			this.specifier = specifier;
			this.variant = variant;
		}

		public VariantSpecifier getSpecifier() {
			return specifier;
		}

		public VariantModel getVariant() {
			return variant;
		}

	}

}
