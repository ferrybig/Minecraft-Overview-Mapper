/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.google.common.base.Preconditions;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.nbt.CompoundTag;
import me.ferrybig.java.minecraft.nbt.ListTag;
import me.ferrybig.java.minecraft.nbt.Tag;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache.TextureMapper;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureKey;

/**
 * This is the default image renderer used by the code, this render as a nice
 * overview map
 *
 * @author Fernando
 */
public class FlatImageRenderer implements RegionRenderer {

	/**
	 * Dimensions of a single axis in a chunk section
	 */
	private static final int CHUNK_SECTION_SIZE = 16;

	/**
	 * Amount of chunk sections in a chunk
	 */
	private static final int MAX_CHUNK_SECTIONS = 16;

	/**
	 * Pixels rendered per block
	 */
	private static final int IMAGE_SIZE = 16;

	/**
	 * Amount of bits in a long object
	 */
	private static final int LONG_SIZE = 64;

	/**
	 * Amount of blocks in a chunk section
	 */
	private static final int BLOCKS_IN_CHUNK_SECTION = CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE;
	private static final int BLOCKS_IN_CHUNK_SURFACE = CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE;
	private static final int TOTAL_IMAGE_SIZE = IMAGE_SIZE * IMAGE_SIZE;
	private static final int BLOCKS_IN_REGION = 512;
	private static final int BLOCKS_IN_REGION_MINUS_ONE = BLOCKS_IN_REGION - 1;
	private static final int PIXELS_IN_REGION = BLOCKS_IN_REGION * IMAGE_SIZE;
	private static final int TOTAL_BLOCKS_IN_REGION = BLOCKS_IN_REGION * BLOCKS_IN_REGION;
	private static final int CHUNKS_IN_REGION = 32;

	private static final boolean DEBUG = false;

	private static int clampToByte(int val) {
		return val > 255 ? 255 : val < 0 ? 0 : val;
	}

	private static void readBiomeList(int[] biomes, Tag biomeTag) {
		int[] biomeList = biomeTag.asIntArrayTag().get();
		if (biomeList.length == 0) {
			Arrays.fill(biomes, 0);
		} else {
			System.arraycopy(biomeList, 0, biomes, 0, biomeList.length);
		}
	}

	private static void shade(int[] pixelArray, int shade) {
		for (int i = 0; i < pixelArray.length; i++) {
			int c1 = pixelArray[i];

			int aplha = c1 & 0xff000000; // optimalize by not shifting this
			if (aplha != 0) {
				int red = clampToByte(((c1 >> 16) & 0xff) + shade);
				int green = clampToByte(((c1 >> 8) & 0xff) + shade);
				int blue = clampToByte(((c1 >> 0) & 0xff) + shade);

				pixelArray[i] = aplha | (red << 16) | (green << 8) | blue;
			}
		}
	}

	private static void shadeChunks(int[] dstPixelsCache, byte[] heights, BufferedImage image) {
		for (int x = 0; x < BLOCKS_IN_REGION; x++) {
			int partialHeightIndex = x * BLOCKS_IN_REGION;
			for (int z = 0; z < BLOCKS_IN_REGION; z++) {
				int heightIndex = partialHeightIndex + z;
				int xAxisShade;
				if (x == 0) {
					xAxisShade = heights[heightIndex + BLOCKS_IN_REGION] - heights[heightIndex];
				} else if (x == BLOCKS_IN_REGION_MINUS_ONE) {
					xAxisShade = heights[heightIndex] - heights[heightIndex - BLOCKS_IN_REGION];
				} else {
					xAxisShade = (heights[heightIndex + BLOCKS_IN_REGION] - heights[heightIndex - BLOCKS_IN_REGION]) * 2;
				}
				int zAxisShade;
				if (z == 0) {
					zAxisShade = heights[heightIndex + 1] - heights[heightIndex];
				} else if (z == BLOCKS_IN_REGION_MINUS_ONE) {
					zAxisShade = heights[heightIndex] - heights[heightIndex - 1];
				} else {
					zAxisShade = (heights[heightIndex + 1] - heights[heightIndex - 1]) * 2;
				}
				int totalShade = xAxisShade + zAxisShade;
				if (totalShade != 0) {
					if (totalShade > 10) {
						totalShade = 10;
					}
					if (totalShade < -10) {
						totalShade = -10;
					}
					image.getRaster().getDataElements(x * IMAGE_SIZE, z * IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, dstPixelsCache);
					double adjustedShade = totalShade / 7.0D * 8;
					shade(dstPixelsCache, (int) adjustedShade);
					image.getRaster().setDataElements(x * IMAGE_SIZE, z * IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, dstPixelsCache);
				}
			}
		}
	}

	private final TextureCache textures;
	private final ChunkSection emptyChunkSection;

	private final BiomeMap biomeMap;

	public FlatImageRenderer(TextureCache textures, BiomeMap biomeMap) {
		this.textures = textures;
		this.biomeMap = biomeMap;
		try {
			this.emptyChunkSection = new ChunkSection(
				1,
				new long[BLOCKS_IN_CHUNK_SECTION / LONG_SIZE],
				new TextureMapper[]{textures.get("air", Collections.emptyMap(), 0)},
				null,
				true
			);
		} catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public RenderOutput renderFile(PreparedFile file, int lastModified) throws IOException, NBTException {
		ChunkReader reader = file.getChunkReader();
		if (lastModified != Integer.MIN_VALUE && lastModified >= reader.getLastModificationDate()) {
			return new RenderOutput(null, lastModified);
		}

		BufferedImage regionDetailImage = new BufferedImage(PIXELS_IN_REGION, BLOCKS_IN_REGION * IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		int[] dstPixelsCache = new int[TOTAL_IMAGE_SIZE];
		ChunkSection[] chunkSectionCache = new ChunkSection[MAX_CHUNK_SECTIONS];
		byte[] heights = new byte[TOTAL_BLOCKS_IN_REGION];
		Arrays.fill(heights, Byte.MIN_VALUE);
		int[] biomeCache = new int[BLOCKS_IN_CHUNK_SURFACE];

		// chunkStream comes from ChunkReader, closing is optional
		InputStream chunkStream;
		while ((chunkStream = reader.nextChunk()) != null) {
			CompoundTag globalTag = Tag.fromNbt(new DataInputStream(chunkStream)).asCompoundTag();
			CompoundTag levelTag = globalTag.resolve("Level").asCompoundTag();
			int globalX = levelTag.resolve("xPos").asIntTag().getValue();
			int globalZ = levelTag.resolve("zPos").asIntTag().getValue();
			int regionX = calculateChunkPos(globalX);
			int regionZ = calculateChunkPos(globalZ);

			try {
				if (DEBUG) {
					System.out.println("Render chunk: " + regionX + "," + regionZ);
				}
				renderChunk(levelTag, regionX, regionZ, regionDetailImage, heights, dstPixelsCache, biomeCache, chunkSectionCache);
			} catch (ExecutionException ex) {
				throw new IOException(ex);
			}
		}
		FlatImageRenderer.shadeChunks(dstPixelsCache, heights, regionDetailImage);
		return new RenderOutput(regionDetailImage, reader.getLastModificationDate());
	}

	private static int calculateChunkPos(int rawChunkLocation) {
		rawChunkLocation %= 32;
		if (rawChunkLocation < 0) {
			rawChunkLocation += 32;
		}
		return rawChunkLocation;
	}

	private void renderChunk(
		CompoundTag levelTag,
		int localX,
		int localZ,
		BufferedImage image,
		byte[] heights,
		int[] dstPixelsCache,
		int[] biomes,
		ChunkSection[] chunkSections
	) throws ExecutionException {
		ListTag sections = (ListTag) levelTag.resolveOrNull("Sections");
		if (sections == null) {
			return; // This is an empty chunk
		}
		FlatImageRenderer.readBiomeList(biomes, levelTag.resolve("Biomes").asIntArrayTag());
		int maxY = this.createChunkPallete(sections, chunkSections, biomes);
		FlatImageRenderer.renderChunkBlocks(localX, localZ, maxY, image, chunkSections, heights, biomes, dstPixelsCache);
	}

	private int createChunkPallete(ListTag sections, ChunkSection[] chunkSections, int[] biomes) throws ExecutionException {
		Arrays.fill(chunkSections, this.emptyChunkSection);
		int maxY = 0;
		for (Tag root : sections) {
			ListTag pallete = root.resolve("Palette").asListTag();
			TextureMapper[] parsedPallete = new TextureMapper[pallete.size()];
			TextureMapper[][] biomePallete = null;
			long[] blockStates = root.resolve("BlockStates").asLongArrayTag().get();

			for (int i = 0; i < pallete.size(); i++) {
				Tag palleteRoot = pallete.get(i);
				String blockId = palleteRoot.resolve("Name").asStringTag().get();
				Map<String, String> properties;
				CompoundTag propertiesNode = (CompoundTag) palleteRoot.resolveOrNull("Properties");
				if (propertiesNode != null) {
					properties = new LinkedHashMap<>();
					Iterator<? extends Map.Entry<String, ? extends Tag>> iterator = propertiesNode.get().entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, ? extends Tag> next = iterator.next();
						properties.put(next.getKey(), next.getValue().asStringTag().get());
					}
				} else {
					properties = Collections.emptyMap();
				}
				int firstBiomeId = biomes[0];
				TextureKey key = TextureKey.of(blockId, properties, firstBiomeId);
				boolean requiresBiomePallete = false;
				if (key.hasBiomeId()) {
					for (int j = 0; j < biomes.length; j++) {
						if (biomes[j] != firstBiomeId) {
							requiresBiomePallete = true;
							break;
						}
					}
				}
				if (requiresBiomePallete) {
					if (biomePallete == null) {
						biomePallete = new TextureMapper[256][];
					}
					for (int z = 0; z < CHUNK_SECTION_SIZE; z++) {
						for (int x = 0; x < CHUNK_SECTION_SIZE; x++) {
							int biome = biomes[z * 16 + x];
							if (biomePallete[biome] == null) {
								biomePallete[biome] = new TextureMapper[pallete.size()];
							}
							biomePallete[biome][i] = this.textures.get(blockId, properties, biome);
						}
					}
				} else {
					parsedPallete[i] = this.textures.get(key);
				}
			}
			int y = root.resolve("Y").asByteTag().get();
			maxY = Math.max(maxY, y * CHUNK_SECTION_SIZE + 15);
			chunkSections[y] = new ChunkSection(
				blockStates.length * LONG_SIZE / (BLOCKS_IN_CHUNK_SECTION),
				blockStates,
				parsedPallete,
				biomePallete,
				false
			);
		}
		return maxY;
	}

	private static void renderChunkBlocks(int localX, int localZ, int maxY, BufferedImage image, ChunkSection[] chunkSections, byte[] heights, int[] biomes, int[] dstPixels) {
		//System.out.println("CHunk at " + localX + "," + localZ + " has max height " + maxY);
		for (int x = 0; x < CHUNK_SECTION_SIZE; x++) {
			int partialHeightMapIndex = (localX * CHUNK_SECTION_SIZE + x) * BLOCKS_IN_REGION;
			for (int z = 0; z < CHUNK_SECTION_SIZE; z++) {
				int fullHeightMapIndex = partialHeightMapIndex + localZ * CHUNK_SECTION_SIZE + z;
				int biome = biomes[z * 16 + x];
				boolean hasCutOffHeightMap = false;
				int y = maxY;
				for (; y > 0; y--) {
					int sectionId = y / CHUNK_SECTION_SIZE;
					int blockY = y % CHUNK_SECTION_SIZE;
					ChunkSection s = chunkSections[sectionId];
					if (s.isEmpty()) {
						y -= 15;
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z, biome);
					//System.out.println("Block at " + x + ',' + y + ',' + z + " is " + block.getBlock() + " and is " + block.isOpaque());
					if (block.isOpaque()) {
						break;
					}
					if (!hasCutOffHeightMap && block.cutOffHeightMap()) {
						heights[fullHeightMapIndex] = (byte) (y - 128);
						hasCutOffHeightMap = true;
					}
				}
				if (!hasCutOffHeightMap) {
					heights[fullHeightMapIndex] = (byte) (y - 128);
				}
				//System.out.println("Blockcolumn at " + x + "," + z + " has min height " + y);

				Arrays.fill(dstPixels, 0);
				for (; y <= maxY; y++) {
					int sectionId = y / CHUNK_SECTION_SIZE;
					int blockY = y % CHUNK_SECTION_SIZE;
					ChunkSection s = chunkSections[sectionId];
					if (s.isEmpty()) {
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z, biome);
					block.apply(dstPixels);
				}
				image.getRaster().setDataElements((x + localX * CHUNK_SECTION_SIZE) * IMAGE_SIZE,
					(z + localZ * CHUNK_SECTION_SIZE) * IMAGE_SIZE,
					IMAGE_SIZE,
					IMAGE_SIZE,
					dstPixels
				);
			}
		}
	}

	private static class ChunkSection {

		private final int blockPartSize;
		private final long blockMask;
		@Nonnull
		private final long[] blockStates;
		@Nullable
		private final TextureMapper[][] biomeStates;
		private final boolean empty;
		@Nonnull
		private final TextureMapper[] states;

		public ChunkSection(int blockPartSize, long[] blockStates, TextureMapper[] states, TextureMapper[][] biomeStates, boolean empty) {
			if (blockStates.length * LONG_SIZE != blockPartSize * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE) {
				throw new IllegalArgumentException("blockStates length does not match expected: "
					+ blockStates.length * LONG_SIZE
					+ " vs "
					+ blockPartSize * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE
				);
			}
			this.blockPartSize = blockPartSize;
			this.blockMask = (1l << blockPartSize) - 1;
			this.blockStates = blockStates;
			this.states = states;
			this.biomeStates = biomeStates;
			this.empty = empty;
		}

		public TextureMapper getBlock(int x, int y, int z, int biome) {
			Preconditions.checkElementIndex(x, CHUNK_SECTION_SIZE, "x");
			Preconditions.checkElementIndex(y, CHUNK_SECTION_SIZE, "y");
			Preconditions.checkElementIndex(z, CHUNK_SECTION_SIZE, "z");
			int blockPos = y * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE + z * CHUNK_SECTION_SIZE + x;
			int bitOffset = blockPos * this.blockPartSize;
			int arrayIndex = bitOffset / LONG_SIZE;
			long arrayValue = this.blockStates[arrayIndex];
			int shiftOffset = bitOffset - (arrayIndex * LONG_SIZE);
			long movedValue = arrayValue >>> shiftOffset;
			long maskedValue = movedValue & this.blockMask;
			if (shiftOffset + this.blockPartSize > LONG_SIZE) {
				int missingMaskSize = shiftOffset + this.blockPartSize - LONG_SIZE;
				int actualReadMaskSize = this.blockPartSize - missingMaskSize;
				long missingPart = this.blockStates[arrayIndex + 1];
				long missingMask = (1l << missingMaskSize) - 1;
				maskedValue |= (missingPart & missingMask) << actualReadMaskSize;
			}
			if (maskedValue >= states.length) {
				throw new IllegalArgumentException("Calculated block id is larger than the pallete allows: " + maskedValue);
			}
			if (maskedValue < 0) {
				throw new IllegalArgumentException("Calculated block id is smaller than the pallete allows: " + maskedValue);
			}
			TextureMapper texture = this.states[(int) maskedValue];
			if (texture == null && this.biomeStates != null) {
				texture = biomeStates[biome][(int) maskedValue];
			}
			if (texture == null) {
				throw new IllegalArgumentException("Block " + maskedValue + " is null in the pallete");
			}
			return texture;
		}

		public boolean isEmpty() {
			return empty;
		}

	}
}
