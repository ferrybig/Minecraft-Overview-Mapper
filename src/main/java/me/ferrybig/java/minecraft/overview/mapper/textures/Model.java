/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.List;
import java.util.Map;
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

}
