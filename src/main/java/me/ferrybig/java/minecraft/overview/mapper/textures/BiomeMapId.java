/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Map;
import javax.annotation.Nullable;

public enum BiomeMapId {
	WATER,
	GRASS,
	LEAVES,;

	@Nullable
	public static BiomeMapId getForBlock(String block, Map<String, String> state) {
		switch (block) {
			case "minecraft:grass": // Pre-flatening
			case "minecraft:grass_block":
				return GRASS;
			case "minecraft:leaves": // Pre-flatening
			case "minecraft:leaves2": // Pre-flatening
			case "minecraft:oak_leaves":
			case "minecraft:spruce_leaves":
			case "minecraft:birch_leaves":
			case "minecraft:jungle_leaves":
			case "minecraft:acacia_leaves":
			case "minecraft:dark_oak_leaves":
				return LEAVES;
			case "minecraft:flowing_water": // Pre-flatening
			case "minecraft:water":
			case "minecraft:bubble_column":
				return WATER;
			default:
				if ("true".equals(state.get("waterlogged"))) {
					return WATER;
				}
				return null;
		}
	}
}
