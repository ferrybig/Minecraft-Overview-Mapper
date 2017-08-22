/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Fernando
 */
public class BiomeMap {

	public static final int INDEX_MASK = 255;
	public static final int SIZE = 256;
	public final Biome defaultBiome;
	public final Biome[] biomes;

	public BiomeMap(Biome[] biomes, Biome defaultBiome) {
		this.biomes = biomes;
		this.defaultBiome = defaultBiome;
	}

	protected static BiomeMap load(BufferedReader paramBufferedReader, String paramString) throws IOException {
		Biome[] arrayOfBiome = new Biome[256];
		Biome localBiome = null;
		int i = 0;
		String str;
		while ((str = paramBufferedReader.readLine()) != null) {
			i++;
			if ((!str.trim().isEmpty())
					&& (!str.trim().startsWith("#"))) {
				String[] arrayOfString = str.split("\t", 5);
				if (arrayOfString.length < 4) {
					System.err.println("Invalid biome map line at " + paramString + ":" + i + ": " + str);
				} else {
					int j = parseInt(arrayOfString[1]);
					int k = parseInt(arrayOfString[2]);
					int m = parseInt(arrayOfString[3]);
					if ("default".equals(arrayOfString[0])) {
						localBiome = new Biome(j, k, m, true);
					} else {
						int n = parseInt(arrayOfString[0]);
						arrayOfBiome[n] = new Biome(j, k, m, false);
					}
				}
			}
		}
		if (localBiome == null) {
			localBiome = new Biome(-65281, -65281, -65281, true);
		}
		return new BiomeMap(arrayOfBiome, localBiome);
	}

	private static int parseInt(String paramString) {
		if (paramString.startsWith("0x")) {
			return (int) Long.parseLong(paramString.substring(2), 16);
		}
		return (int) Long.parseLong(paramString);
	}

	public static BiomeMap loadDefault() throws IOException {
		try (BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(BiomeMap.class.getResourceAsStream("biome-colors.txt")))) {
			return load(localBufferedReader, "(default biome colors)");
		}

	}

	public static BiomeMap load(File paramFile) throws IOException {
		try (BufferedReader localBufferedReader = new BufferedReader(new FileReader(paramFile))) {
			return load(localBufferedReader, paramFile.getPath());
		}
	}

	public Biome getBiome(int paramInt) {
		if ((paramInt >= 0) && (paramInt < this.biomes.length)
				&& (this.biomes[paramInt] != null)) {
			return this.biomes[paramInt];
		}

		return this.defaultBiome;
	}

	public static final class Biome {

		public final int grassColor;
		public final int foliageColor;
		public final int waterColor;
		public final boolean isDefault;

		public Biome(int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean) {
			this.grassColor = paramInt1;
			this.foliageColor = paramInt2;
			this.waterColor = paramInt3;
			this.isDefault = paramBoolean;
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
	}
}
