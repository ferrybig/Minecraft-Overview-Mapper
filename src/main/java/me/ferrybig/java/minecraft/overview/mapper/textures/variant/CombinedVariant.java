/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.ferrybig.java.minecraft.overview.mapper.textures.Model;

public class CombinedVariant implements Variant {

	private final List<Variant> variants;

	public CombinedVariant(List<Variant> variants) {
		this.variants = variants;
	}

	@Override
	public Stream<Model> stream() {
		return variants.stream().flatMap(Variant::stream);
	}

	@Override
	public List<Model> getModels() {
		return this.stream().collect(Collectors.toList());
	}

	@Override
	public Iterator<Model> iterator() {
		return this.stream().iterator();
	}

}
