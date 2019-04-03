/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Model {

	@Nonnull
	private final List<Cube> elements;
	@Nonnull
	private final Map<String, String> texture;
	@Nullable
	private final Model parent;

	public Model(List<Cube> elements, Map<String, String> texture, Model parent) {
		this.elements = elements;
		this.texture = texture;
		this.parent = parent;
	}

	@Nonnull
	public List<Cube> getElements() {
		return elements;
	}

	@Nonnull
	public Map<String, String> getTexture() {
		return texture;
	}

	@Nullable
	public Model getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "Model{" + "elements=" + elements + ", texture=" + texture + ", parent=" + parent + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 59 * hash + Objects.hashCode(this.elements);
		hash = 59 * hash + Objects.hashCode(this.texture);
		hash = 59 * hash + Objects.hashCode(this.parent);
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
		final Model other = (Model) obj;
		if (!Objects.equals(this.elements, other.elements)) {
			return false;
		}
		if (!Objects.equals(this.texture, other.texture)) {
			return false;
		}
		if (!Objects.equals(this.parent, other.parent)) {
			return false;
		}
		return true;
	}

}
