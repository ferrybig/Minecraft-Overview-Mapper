/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.blockstate;

import java.util.Map;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;

public interface UnresolvedBlockState {

	@Nonnull
	public Variant resolve(@Nonnull Map<String, String> state);
}
