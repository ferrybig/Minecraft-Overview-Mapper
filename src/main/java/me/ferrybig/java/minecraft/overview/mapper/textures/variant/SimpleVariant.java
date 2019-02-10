/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class SimpleVariant implements Variant {

	private final List<VariantModel> models;

	public SimpleVariant(List<VariantModel> models) {
		this.models = models;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 67 * hash + Objects.hashCode(this.models);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SimpleVariant other = (SimpleVariant) obj;
		if (!Objects.equals(this.models, other.models)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SimpleVariant{" + "models=" + models + '}';
	}

	@Override
	public List<VariantModel> getModels() {
		return models;
	}

	@Override
	public Iterator<VariantModel> iterator() {
		return models.iterator();
	}

}
