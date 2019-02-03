/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import java.util.Map;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import java.util.SortedMap;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.CombinedVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantSpecifier;

public interface UnresolvedBlockState {
	public Variant resolve(Map<String, String> state);

	public static UnresolvedBlockState of(Map<VariantSpecifier, Variant> list) {
		int size = list.size();
		switch(size) {
			case 0:
				return Variant.empty();
			case 1:
				return list.values().iterator().next();
			default:
				return new MultiBlockState(list);
		}
	}
}
