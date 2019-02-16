/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier;

import java.util.Map;

public interface VariantSpecifier {

	public boolean matches(Map<String, String> state);

	public static VariantSpecifier TRUE = (Map<String, String> state) -> true;

	public static VariantSpecifier FALSE = (Map<String, String> state) -> false;
}
