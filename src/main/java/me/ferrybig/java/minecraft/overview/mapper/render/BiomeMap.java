/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.overview.mapper.textures.BiomeMapId;

/**
 *
 * @author Fernando
 */
public class BiomeMap {

	private static final String DEFAULT_BIOME_STRING = "default";
	@Nonnull
	private final Biome defaultBiome;
	@Nonnull
	private final Biome[] biomes;

	private BiomeMap(@Nonnull Biome[] biomes, @Nonnull Biome defaultBiome) {
		this.biomes = biomes;
		this.defaultBiome = defaultBiome;
	}

	@Nonnull
	protected static BiomeMap load(@Nonnull BufferedReader reader, @Nullable String fileName) throws IOException {
		Biome[] biomeMap = new Biome[256];
		Biome defaultBiome = null;
		int lineNumber = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			lineNumber++;
			String lineTrimmed = line.trim();
			if ((!lineTrimmed.isEmpty()) && (!lineTrimmed.startsWith("#"))) {
				String[] split = line.split("\t", 5);
				if (split.length < 4) {
					throw new IOException("Invalid biome map line at " + fileName + ":" + lineNumber + ": " + line);
				} else {
					int grassColor = parseNumber(split[1]);
					int foliageColor = parseNumber(split[2]);
					int waterColor = parseNumber(split[3]);
					if (DEFAULT_BIOME_STRING.equals(split[0])) {
						defaultBiome = new Biome(grassColor, foliageColor, waterColor, true);
					} else {
						int biomeId = parseNumber(split[0]);
						biomeMap[biomeId] = new Biome(grassColor, foliageColor, waterColor, false);
					}
				}
			}
		}
		if (defaultBiome == null) {
			defaultBiome = new Biome(-65281, -65281, -65281, true);
		}
		return new BiomeMap(biomeMap, defaultBiome);
	}

	private static int parseNumber(String input) {
		if (input.startsWith("0x")) {
			return (int) Long.parseLong(input.substring(2), 16);
		}
		return (int) Long.parseLong(input);
	}

	@Nonnull
	public static BiomeMap loadDefault() throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(BiomeMap.class.getResourceAsStream("biome-colors.txt")))) {
			return load(reader, "(default biome colors)");
		}

	}

	@Nonnull
	public static BiomeMap load(File file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			return load(reader, file.getPath());
		}
	}

	@Nonnull
	public Biome getBiome(int biomeId) {
		if ((biomeId >= 0) && (biomeId < this.biomes.length)
			&& (this.biomes[biomeId] != null)) {
			return this.biomes[biomeId];
		}

		return this.defaultBiome;
	}

	public int getBiomeColor(int biomeId, @Nonnull BiomeMapId mapId) {
		return this.getBiome(biomeId).getMultiplier(mapId);
	}

	public static final class Biome {

		private final int grassColor;
		private final int foliageColor;
		private final int waterColor;
		private final boolean isDefault;

		private Biome(int grassColor, int foliageColor, int waterColor, boolean isDefaultBiome) {
			this.grassColor = grassColor;
			this.foliageColor = foliageColor;
			this.waterColor = waterColor;
			this.isDefault = isDefaultBiome;
		}

		public int getMultiplier(int paramInt) {
			switch (paramInt) {
				case 1:
					return this.grassColor;
				case 2:
					return this.foliageColor;
				case 3:
					return this.waterColor;
			}
			return -1;
		}

		public int getMultiplier(BiomeMapId mapId) {
			switch (mapId) {
				case GRASS:
					return this.grassColor;
				case LEAVES:
					return this.foliageColor;
				case WATER:
					return this.waterColor;
				default:
					throw new IllegalArgumentException("Unknown map id: " + mapId);
			}
		}
	}
}
