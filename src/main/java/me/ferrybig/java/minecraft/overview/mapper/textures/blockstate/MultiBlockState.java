/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.SimpleVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.VariantSpecifier;

public class MultiBlockState implements UnresolvedBlockState {

	@Nonnull
	private final List<Entry> partList;

	public MultiBlockState(@Nonnull Map<VariantSpecifier, VariantModel> list) {
		this.partList = list.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	public MultiBlockState(@Nonnull List<Map.Entry<VariantSpecifier, VariantModel>> list) {
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

		@Nonnull
		private final VariantSpecifier specifier;
		@Nonnull
		private final VariantModel variant;

		public Entry(@Nonnull VariantSpecifier specifier, @Nonnull VariantModel variant) {
			this.specifier = Objects.requireNonNull(specifier, "specifier");
			this.variant = Objects.requireNonNull(variant, "variant");
		}

		@Nonnull
		public VariantSpecifier getSpecifier() {
			return specifier;
		}

		@Nonnull
		public VariantModel getVariant() {
			return variant;
		}

	}

}
