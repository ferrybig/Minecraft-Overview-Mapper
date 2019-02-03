/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import me.ferrybig.java.minecraft.overview.mapper.textures.Model;

public class DefaultVariant implements Variant {
	private final List<Model> modelList;

	public DefaultVariant(List<Model> modelList) {
		this.modelList = modelList;
	}

	@Override
	public List<Model> getModels() {
		return modelList;
	}

	@Override
	public Iterator<Model> iterator() {
		return modelList.iterator();
	}

	@Override
	public Spliterator<Model> spliterator() {
		return modelList.spliterator();
	}


}
