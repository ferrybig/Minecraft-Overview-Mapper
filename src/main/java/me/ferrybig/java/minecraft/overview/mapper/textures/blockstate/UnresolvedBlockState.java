/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import java.util.Map;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;

public interface UnresolvedBlockState {

	public Variant resolve(Map<String, String> state);
}
