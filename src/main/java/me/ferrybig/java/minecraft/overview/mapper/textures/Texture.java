/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Objects;

public class Texture {
	private final String texture;
	private final String cullFace;

	public Texture(String texture, String cullFace) {
		this.texture = texture;
		this.cullFace = cullFace;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + Objects.hashCode(this.texture);
		hash = 53 * hash + Objects.hashCode(this.cullFace);
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
		final Texture other = (Texture) obj;
		if (!Objects.equals(this.texture, other.texture)) {
			return false;
		}
		if (!Objects.equals(this.cullFace, other.cullFace)) {
			return false;
		}
		return true;
	}

	public String getTexture() {
		return texture;
	}

	public String getCullFace() {
		return cullFace;
	}

	@Override
	public String toString() {
		return "Texture{" + "texture=" + texture + ", cullFace=" + cullFace + '}';
	}


}
