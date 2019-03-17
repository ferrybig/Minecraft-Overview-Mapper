/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextureKey {

	private static final int BIOME_ID_UNKNOWN = -1;
	private static final BiomeMapId BIOME_MAP_UNKNOWN = null;

	@Nonnull
	private final String block;
	@Nonnull
	private final Map<String, String> state;
	private final int biomeId;
	private final boolean hasBiomeId;
	@Nullable
	private final BiomeMapId biomeMapId;

	private TextureKey(@Nonnull String block, @Nonnull Map<String, String> state, int biomeId, @Nonnull BiomeMapId biomeMapId) {
		this.block = Objects.requireNonNull(block, "block");
		this.state = Objects.requireNonNull(state, "state");
		this.biomeId = biomeId;
		this.hasBiomeId = true;
		this.biomeMapId = Objects.requireNonNull(biomeMapId, "biomeMapId");
	}

	private TextureKey(@Nonnull String block, @Nonnull Map<String, String> state) {
		this.block = block;
		this.state = state;
		this.biomeId = BIOME_ID_UNKNOWN;
		this.hasBiomeId = false;
		this.biomeMapId = BIOME_MAP_UNKNOWN;
	}

	@Nonnull
	public String getBlock() {
		return block;
	}

	@Nonnull
	public Map<String, String> getState() {
		return state;
	}

	public int getBiomeId() {
		return biomeId;
	}

	public boolean hasBiomeId() {
		return hasBiomeId;
	}

	@Nullable
	public BiomeMapId getBiomeMapId() {
		return biomeMapId;
	}

	@Override
	public String toString() {
		return "TextureKey{" + "block=" + block + ", state=" + state + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(this.block);
		hash = 89 * hash + Objects.hashCode(this.state);
		hash = 89 * hash + this.biomeId;
		hash = 89 * hash + Objects.hashCode(this.biomeMapId);
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
		if (this.biomeId != other.biomeId) {
			return false;
		}
		if (!Objects.equals(this.block, other.block)) {
			return false;
		}
		if (!Objects.equals(this.state, other.state)) {
			return false;
		}
		if (this.biomeMapId != other.biomeMapId) {
			return false;
		}
		return true;
	}

	@Nonnull
	public static TextureKey of(String block, Map<String, String> state, int biomeId) {
		BiomeMapId biomeMapId = BiomeMapId.getForBlock(block, state);
		if (biomeMapId == null) {
			return new TextureKey(block, state);
		} else {
			return new TextureKey(block, state, biomeId, biomeMapId);
		}
	}

}
