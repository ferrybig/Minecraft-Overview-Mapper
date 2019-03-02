/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache;
import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.google.common.base.Preconditions;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap.Biome;
import me.ferrybig.java.minecraft.overview.mapper.render.BlockMap.Block;
import me.ferrybig.java.minecraft.overview.mapper.streams.ByteCountingDataInputStream;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache.TextureMapper;

/**
 * This is the default image renderer used by the code, this render as a nice
 * overview map
 *
 * @author Fernando
 */
public class FlatImageRenderer implements RegionRenderer {

	/**
	 * Chunk is in the GZIP format
	 */
	private static final int VERSION_GZIP = 1;
	/**
	 * Chunk is in the deflate format
	 */
	private static final int VERSION_DEFLATE = 2;

	/**
	 * Size of a kibibyte
	 */
	private static final int KIBIBYTE = 1024;

	/**
	 * Size of a sector in a region file
	 */
	private static final int SECTOR_BYTES = 4096;
	/**
	 * Amount of integers present in an "integer" section of an region file sector
	 */
	private static final int SECTOR_INTS = SECTOR_BYTES / 4;

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
	public static final int LONG_SIZE = 64;


	private final TextureCache textures;
	private final ChunkSection emptyChunkSection;

	private final BiomeMap biomeMap;

	public FlatImageRenderer(TextureCache textures, BiomeMap biomeMap) {
		this.textures = textures;
		this.biomeMap = biomeMap;
		try {
			this.emptyChunkSection = new ChunkSection(
				1,
				new long[CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE / LONG_SIZE],
				new TextureMapper[]{textures.get("air", Collections.emptyMap())},
				true
			);
		} catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public BufferedImage renderFile(String fileName, InputStream input) throws IOException {
		ByteCountingDataInputStream in = new ByteCountingDataInputStream(new BufferedInputStream(input, SECTOR_BYTES * 2));
		PriorityQueue<Integer> chunkIndexes = new PriorityQueue<>(Integer::compare);
		for (int k = 0; k < SECTOR_INTS; ++k) {
			int offset = in.readInt();
			if (offset != 0) {
				int sectorNumber = offset >> 8;
				chunkIndexes.add(sectorNumber * SECTOR_BYTES);
			}
		}
		assert in.getReadBytes() == SECTOR_BYTES;
		this.skipFully(in, SECTOR_BYTES); // Skip timestamp sector
		assert in.getReadBytes() == SECTOR_BYTES * 2;

		BufferedImage regionDetailImage = new BufferedImage(512 * CHUNK_SECTION_SIZE, 512 * CHUNK_SECTION_SIZE, BufferedImage.TYPE_INT_ARGB);
		Integer lowestChunkIndex;
		CompoundTag globalTag;
		CompoundTag levelTag;
		byte[] data = new byte[4 * KIBIBYTE];
		Graphics2D g2 = regionDetailImage.createGraphics();
		try {
			while ((lowestChunkIndex = chunkIndexes.poll()) != null) {
				long chunkIndex = lowestChunkIndex;
				long bytesRead = in.getReadBytes();
				if (bytesRead > chunkIndex) {
					throw new IOException("We already read " + bytesRead + " but a chunk exisists at byte index " + chunkIndex);
				} else if (bytesRead < chunkIndex) {
					this.skipFully(in, chunkIndex - bytesRead);
				}
				assert in.getReadBytes() == chunkIndex;
				int chunkLength = in.readInt();
				if (data.length < chunkLength) {
					data = new byte[(chunkLength / KIBIBYTE + 1) * KIBIBYTE];
				}
				in.readFully(data, 0, chunkLength);
				byte version = data[0];
				// Don't close chunkstream as its based on a ByteArrayInputStream object
				InputStream chunkStream = new ByteArrayInputStream(data, 1, data.length - 1);
				switch (version) {
					case VERSION_GZIP:
						chunkStream = new GZIPInputStream(chunkStream);
						break;
					case VERSION_DEFLATE:
						chunkStream = new InflaterInputStream(chunkStream);
						break;
					default:
						throw new IOException("Invalid format: " + version);
				}
				globalTag = (CompoundTag) new NBTInputStream(
					new DataInputStream(chunkStream), false).readTag();
				levelTag = (CompoundTag) globalTag.getValue().get("Level");
				int globalX = ((IntTag) levelTag.getValue().get("xPos")).getValue();
				int globalZ = ((IntTag) levelTag.getValue().get("zPos")).getValue();
				int regionX = calculateChunkPos(globalX);
				int regionZ = calculateChunkPos(globalZ);

				try {
					System.out.println("Render chunk: " + regionX + "," + regionZ);
					renderChunk(levelTag, regionX, regionZ, g2, regionDetailImage);
				} catch (ExecutionException ex) {
					throw new IOException(ex);
				}
			}
		} finally {
			g2.dispose();
		}
		//demultiplyAlpha(imageColorArray);
		//shade(imageShadeArray, imageColorArray);

		//BufferedImage localBufferedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		//for (int k = 0; k < 512; k++) {
		//	localBufferedImage.setRGB(0, k, 512, 1, imageColorArray, 512 * k, 512);
		//}
		//return localBufferedImage;
		return regionDetailImage;
	}

	private static void shade(short[] imageColorArray, int[] imageShadeArray) {
		int xMax = 512;
		int zMax = 512;

		int index = 0;
		for (int z = 0; z < zMax; z++) {
			for (int x = 0; x < xMax; x++, index++) {
				if (imageShadeArray[index] != 0) {
					float xAdjustment;
					if (x == 0) {
						xAdjustment = imageColorArray[(index + 1)] - imageColorArray[index];
					} else if (x == xMax - 1) {
						xAdjustment = imageColorArray[index] - imageColorArray[(index - 1)];
					} else {
						xAdjustment = (imageColorArray[(index + 1)] - imageColorArray[(index - 1)]) * 2;
					}
					float zAdjustment;
					if (z == 0) {
						zAdjustment = imageColorArray[(index + xMax)] - imageColorArray[index];
					} else if (z == zMax - 1) {
						zAdjustment = imageColorArray[index] - imageColorArray[(index - xMax)];
					} else {
						zAdjustment = (imageColorArray[(index + xMax)] - imageColorArray[(index - xMax)]) * 2;
					}
					float totalAdjustment = xAdjustment + zAdjustment;
					if (totalAdjustment > 10.0F) {
						totalAdjustment = 10.0F;
					}
					if (totalAdjustment < -10.0F) {
						totalAdjustment = -10.0F;
					}

					totalAdjustment = (float) (totalAdjustment + (imageColorArray[index] - LONG_SIZE) / 7.0D);

					imageShadeArray[index] = Color.shade(imageShadeArray[index], (int) (totalAdjustment * 8.0F));
				}

			}
		}
	}

	private static int getColor(BlockMap blockMap, BiomeMap biomeMap, int blockId, int blockDatum, int biomeId) {
		assert blockId >= 0 && blockId < blockMap.blocks.length;
		assert blockDatum >= 0;

		int blockColor;
		int biomeInfluence;

		Block bc = blockMap.blocks[blockId];
		if (bc.hasSubColors.length > blockDatum && bc.hasSubColors[blockDatum]) {
			blockColor = bc.subColors[blockDatum];
			biomeInfluence = bc.subColorInfluences[blockDatum];
		} else {
			blockColor = bc.baseColor;
			biomeInfluence = bc.baseInfluence;
		}
		if (bc.isDefault) {
//            System.out.println("Unknown block: " + blockId);
		}

		Biome biome = biomeMap.getBiome(biomeId);
		int biomeColor = biome.getMultiplier(biomeInfluence);

		return Color.multiplySolid(blockColor, biomeColor);

	}

	private static int calculateChunkPos(int rawChunkLocation) {
		rawChunkLocation %= 32;
		if (rawChunkLocation < 0) {
			rawChunkLocation += 32;
		}
		return rawChunkLocation;
	}

	private void renderChunk(CompoundTag levelTag, int localX, int localZ, Graphics2D g2, BufferedImage image) throws ExecutionException {
		ListTag<?> sections = (ListTag<?>) levelTag.getValue().get("Sections");
		ChunkSection[] chunkSections = new ChunkSection[CHUNK_SECTION_SIZE];
		Arrays.fill(chunkSections, this.emptyChunkSection);
		int maxY = 0;
		for (Object section : sections.getValue()) {
			CompoundMap root = ((CompoundTag) section).getValue();
			List<?> pallete = ((ListTag<?>) root.get("Palette")).getValue();
			TextureMapper[] parsedPallete = new TextureMapper[pallete.size()];
			long[] blockStates = ((LongArrayTag) root.get("BlockStates")).getValue();

			for (int i = 0; i < pallete.size(); i++) {
				CompoundMap palleteRoot = ((CompoundTag) pallete.get(i)).getValue();
				String blockId = ((StringTag) palleteRoot.get("Name")).getValue();
				Map<String, String> properties;
				if (palleteRoot.containsKey("Properties")) {
					properties = new LinkedHashMap<>();
					CompoundMap map = ((CompoundTag) palleteRoot.get("Properties")).getValue();
					Iterator<Map.Entry<String, Tag<?>>> iterator = map.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, Tag<?>> next = iterator.next();
						properties.put(next.getKey(), ((StringTag) next.getValue()).getValue());
					}
				} else {
					properties = Collections.emptyMap();
				}
				parsedPallete[i] = this.textures.get(blockId, properties);
			}
			int y = ((ByteTag) root.get("Y")).getValue();
			maxY = Math.max(maxY, y * CHUNK_SECTION_SIZE + 15);
			chunkSections[y] = new ChunkSection(
				blockStates.length * LONG_SIZE / (CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE),
				blockStates,
				parsedPallete,
				false
			);
		}
		//System.out.println("CHunk at " + localX + "," + localZ + " has max height " + maxY);
		for (int x = 0; x < CHUNK_SECTION_SIZE; x++) {
			for (int z = 0; z < CHUNK_SECTION_SIZE; z++) {
				int y = maxY;
				for (; y > 0; y--) {
					int sectionId = y / CHUNK_SECTION_SIZE;
					int blockY = y % CHUNK_SECTION_SIZE;
					ChunkSection s = chunkSections[sectionId];
					if (s.isEmpty()) {
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z);
					//System.out.println("Block at " + x + ',' + y + ',' + z + " is " + block.getBlock() + " and is " + block.isOpaque());
					if (block.isOpaque()) {
						break;
					}
				}
				//System.out.println("Blockcolumn at " + x + "," + z + " has min height " + y);

				int[] dstPixels = new int[IMAGE_SIZE * IMAGE_SIZE];
				for (; y <= maxY; y++) {
					int sectionId = y / CHUNK_SECTION_SIZE;
					int blockY = y % CHUNK_SECTION_SIZE;
					ChunkSection s = chunkSections[sectionId];
					if (s.isEmpty()) {
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z);
					block.apply(dstPixels);
				}
				image.setRGB(
					(x + localX * CHUNK_SECTION_SIZE) * IMAGE_SIZE,
					(z + localZ * CHUNK_SECTION_SIZE) * IMAGE_SIZE,
					IMAGE_SIZE,
					IMAGE_SIZE,
					dstPixels,
					0,
					IMAGE_SIZE
				);
			}
		}

	}

	private void skipFully(InputStream in, long n) throws IOException {
		long total = 0;
		long cur = 0;

		while ((total < n) && ((cur = in.skip(n - total)) > 0)) {
			total += cur;
		}
		if (total != n) {
			throw new EOFException();
		}
	}

	private class ChunkSection {

		private final int blockPartSize;
		private final long blockMask;
		private final long[] blockStates;
		private final boolean empty;
		private final TextureMapper[] states;

		public ChunkSection(int blockPartSize, long[] blockStates, TextureMapper[] states, boolean empty) {
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
			this.empty = empty;
		}

		public TextureMapper getBlock(int x, int y, int z) {
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
