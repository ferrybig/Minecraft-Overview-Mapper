/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Map;
import java.util.Objects;

class TextureKey {

	private final String block;
	private final Map<String, String> state;

	public TextureKey(String block, Map<String, String> state) {
		this.block = block;
		this.state = state;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 29 * hash + Objects.hashCode(this.block);
		hash = 29 * hash + Objects.hashCode(this.state);
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
		final TextureKey other = (TextureKey) obj;
		if (!Objects.equals(this.block, other.block)) {
			return false;
		}
		if (!Objects.equals(this.state, other.state)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TextureKey{" + "block=" + block + ", state=" + state + '}';
	}

	public String getBlock() {
		return block;
	}

	public Map<String, String> getState() {
		return state;
	}

}
