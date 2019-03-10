/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.google.common.base.Preconditions;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache.TextureMapper;

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
	public static final int LONG_SIZE = 64;

	/**
	 * Amount of blocks in a chunk section
	 */
	public static final int BLOCKS_IN_CHUNK_SECTION = CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE * CHUNK_SECTION_SIZE;

	private static final boolean DEBUG = false;

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
				new TextureMapper[]{textures.get("air", Collections.emptyMap())},
				true
			);
		} catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public RenderOutput renderFile(PreparedFile file, int lastModified) throws IOException {
		ChunkReader reader = file.getChunkReader();
		if (lastModified != Integer.MIN_VALUE && lastModified >= reader.getLastModificationDate()) {
			return new RenderOutput(null, lastModified);
		}

		BufferedImage regionDetailImage = new BufferedImage(512 * IMAGE_SIZE, 512 * IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		int[] dstPixels = new int[IMAGE_SIZE * IMAGE_SIZE];

		// chunkStream comes from ChunkReader, closing is optional
		InputStream chunkStream;
		while ((chunkStream = reader.nextChunk()) != null) {
			CompoundTag globalTag = (CompoundTag) new NBTInputStream(
				new DataInputStream(chunkStream), false).readTag();
			CompoundTag levelTag = (CompoundTag) globalTag.getValue().get("Level");
			int globalX = ((IntTag) levelTag.getValue().get("xPos")).getValue();
			int globalZ = ((IntTag) levelTag.getValue().get("zPos")).getValue();
			int regionX = calculateChunkPos(globalX);
			int regionZ = calculateChunkPos(globalZ);

			try {
				if (DEBUG) {
					System.out.println("Render chunk: " + regionX + "," + regionZ);
				}
				renderChunk(levelTag, regionX, regionZ, regionDetailImage, dstPixels);
			} catch (ExecutionException ex) {
				throw new IOException(ex);
			}
		}
		return new RenderOutput(regionDetailImage, reader.getLastModificationDate());
	}

	private static int calculateChunkPos(int rawChunkLocation) {
		rawChunkLocation %= 32;
		if (rawChunkLocation < 0) {
			rawChunkLocation += 32;
		}
		return rawChunkLocation;
	}

	private void renderChunk(CompoundTag levelTag, int localX, int localZ, BufferedImage image, int[] dstPixels) throws ExecutionException {
		ListTag<?> sections = (ListTag<?>) levelTag.getValue().get("Sections");
		if (sections == null) {
			return; // This is an empty chunk
		}
		ChunkSection[] chunkSections = new ChunkSection[MAX_CHUNK_SECTIONS];
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
				blockStates.length * LONG_SIZE / (BLOCKS_IN_CHUNK_SECTION),
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
						y -= 15;
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z);
					//System.out.println("Block at " + x + ',' + y + ',' + z + " is " + block.getBlock() + " and is " + block.isOpaque());
					if (block.isOpaque()) {
						break;
					}
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

	private static class ChunkSection {

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
