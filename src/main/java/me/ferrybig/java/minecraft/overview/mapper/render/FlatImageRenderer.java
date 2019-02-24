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
import java.awt.Graphics2D;
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

	private static final int VERSION_GZIP = 1;
	private static final int VERSION_DEFLATE = 2;

	private static final int SECTOR_BYTES = 4096;
	private static final int SECTOR_INTS = SECTOR_BYTES / 4;

	private static final int SHADE_CUTOFF = 32;
	private final static int MAX_CHUNK_SECTIONS = 16;
	private final TextureCache textures;
	private final ChunkSection emptyChunkSection;

	private final BiomeMap biomeMap;

	public FlatImageRenderer(TextureCache textures, BiomeMap biomeMap) {
		this.textures = textures;
		this.biomeMap = biomeMap;
		try {
			this.emptyChunkSection = new ChunkSection(
				1,
				new long[64],
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
		this.skipFully(in, SECTOR_BYTES);
		assert in.getReadBytes() == SECTOR_BYTES * 2;

		short[][] blockId = new short[MAX_CHUNK_SECTIONS][4096];
		byte[][] blockData = new byte[MAX_CHUNK_SECTIONS][4096];
		boolean[] sectionsUsedList = new boolean[MAX_CHUNK_SECTIONS];
		byte[] biomes = new byte[256];
		int[] imageColorArray = new int[512 * 512];
		short[] imageShadeArray = new short[512 * 512];
		int regionZ;
		int regionX;
		int globalX;
		int globalZ;
		long lastUpdate;
		BufferedImage regionDetailImage = new BufferedImage(512 * 16, 512 * 16, BufferedImage.TYPE_INT_ARGB);
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
				globalX = ((IntTag) levelTag.getValue().get("xPos")).getValue();
				globalZ = ((IntTag) levelTag.getValue().get("zPos")).getValue();
				lastUpdate = ((LongTag) levelTag.getValue().get("LastUpdate")).getValue();
				regionX = calculateChunkPos(globalX);
				regionZ = calculateChunkPos(globalZ);

				try {
					System.out.println("Render chunk: " + regionX + "," + regionZ);
					renderChunk(levelTag, regionX, regionZ, g2);
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
	private static final int KIBIBYTE = 1024;

	@SuppressWarnings("unchecked")
	private static void loadChunkData(CompoundTag levelTag, short[][] sectionBlockIds, byte[][] sectionBlockData, boolean[] sectionsUsed, byte[] biomeIds) {
		for (int i = 0; i < MAX_CHUNK_SECTIONS; i++) {
			sectionsUsed[i] = false;
		}
		Tag<?> biomesTag = levelTag.getValue().get("Biomes");
		if (biomesTag != null) {
			System.arraycopy(((IntArrayTag) biomesTag).getValue(), 0, biomeIds, 0, 16 * 16);
		} else {
			for (int i = 0; i < 16 * 16; i++) {
				biomeIds[i] = -1;
			}
		}

		for (CompoundTag sectionInfo : ((ListTag<CompoundTag>) levelTag.getValue().get("Sections")).getValue()) {
			int sectionIndex = ((ByteTag) sectionInfo.getValue().get("Y")).getValue().intValue();
			byte[] blockIdsLow = ((ByteArrayTag) sectionInfo.getValue().get("Blocks")).getValue();
			byte[] blockData = ((ByteArrayTag) sectionInfo.getValue().get("Data")).getValue();
			Tag<?> addTag = sectionInfo.getValue().get("Add");
			byte[] blockAdd = null;
			if (addTag != null) {
				blockAdd = ((ByteArrayTag) addTag).getValue();
			}
			@SuppressWarnings("MismatchedReadAndWriteOfArray")
			short[] destSectionBlockIds = sectionBlockIds[sectionIndex];
			@SuppressWarnings("MismatchedReadAndWriteOfArray")
			byte[] destSectionData = sectionBlockData[sectionIndex];
			sectionsUsed[sectionIndex] = true;
			for (int y = 0; y < 16; ++y) {
				for (int z = 0; z < 16; ++z) {
					for (int x = 0; x < 16; ++x) {
						int index = y * 256 + z * 16 + x;
						short blockType = (short) (blockIdsLow[index] & 0xFF);
						if (blockAdd != null) {
							blockType |= getBlockFromNybbleArray(blockAdd, index) << 8;
						}
						destSectionBlockIds[index] = blockType;
						destSectionData[index] = getBlockFromNybbleArray(blockData, index);
					}
				}
			}
		}
	}

	private static void preRenderChunk(
		short[][] sectionBlockIds, byte[][] sectionBlockData,
		boolean[] usedSections, byte[] biomeIds, BlockMap blockMap,
		BiomeMap biomes, int cx, int cz, int[] colors, short[] heights) {
		/**
		 * Color of 16 air blocks stacked
		 */
		final int air16Color = Color.overlay(0, getColor(blockMap, biomes, 0, 0, 0), 16);
		int maxSectionCount = MAX_CHUNK_SECTIONS;
		for (int s = 0; s < maxSectionCount; ++s) {
			if (usedSections[s]) {
				//++timer.sectionCount;
			}
		}

		//resetInterval();
		for (int z = 0; z < 16; ++z) {
			for (int x = 0; x < 16; ++x) {
				int pixelColor = 0;
				short pixelHeight = 0;
				int biomeId = biomeIds[z * 16 + x] & 0xFF;

				for (int s = 0; s < maxSectionCount; ++s) {
					if (usedSections[s]) {
						short[] blockIds = sectionBlockIds[s];
						byte[] blockData = sectionBlockData[s];

						for (int idx = z * 16 + x, y = 0, absY = s * 16; y < 16; ++y, idx += 256, ++absY) {
							final short blockId = blockIds[idx];
							final byte blockDatum = blockData[idx];
							int blockColor = getColor(blockMap, biomes, blockId & 0xFFFF, blockDatum, biomeId);
							pixelColor = Color.overlay(pixelColor, blockColor);
							if (Color.alpha(blockColor) >= SHADE_CUTOFF) {
								pixelHeight = (short) absY;
							}
						}
					} else {
						pixelColor = Color.overlay(pixelColor, air16Color);
					}
				}

				final int dIdx = 512 * (cz * 16 + z) + 16 * cx + x;
				colors[dIdx] = pixelColor;
				heights[dIdx] = pixelHeight;
			}
		}//pixelColor = new java.awt.Color(cz/32.0f,z/16.0f,cx/32.0f,x/16.0f).getRGB();
	}

	private static void demultiplyAlpha(int[] imageColorArray) {
		for (int i = imageColorArray.length - 1; i >= 0; i--) {
			imageColorArray[i] = Color.demultiplyAlpha(imageColorArray[i]);
		}
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

					totalAdjustment = (float) (totalAdjustment + (imageColorArray[index] - 64) / 7.0D);

					imageShadeArray[index] = Color.shade(imageShadeArray[index], (int) (totalAdjustment * 8.0F));
				}
				
			}
		}
	}

	private static byte getBlockFromNybbleArray(byte[] paramArrayOfByte, int paramInt) {
		return (byte) ((paramInt % 2 == 0 ? paramArrayOfByte[(paramInt / 2)] : paramArrayOfByte[(paramInt / 2)] >> 4) & 0xF);
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

	private void renderChunk(CompoundTag levelTag, int localX, int localZ, Graphics2D g2) throws ExecutionException {
		ListTag<?> sections = (ListTag<?>)levelTag.getValue().get("Sections");
		ChunkSection[] chunkSections = new ChunkSection[16];
		Arrays.fill(chunkSections, this.emptyChunkSection);
		int maxY = 0;
		for(Object section : sections.getValue()) {
			CompoundMap root = ((CompoundTag)section).getValue();
			List<?> pallete = ((ListTag<?>)root.get("Palette")).getValue();
			TextureMapper[] parsedPallete = new TextureMapper[pallete.size()];
			long[] blockStates = ((LongArrayTag)root.get("BlockStates")).getValue();

			for(int i = 0; i < pallete.size(); i++) {
				CompoundMap palleteRoot = ((CompoundTag)pallete.get(i)).getValue();
				String blockId = ((StringTag)palleteRoot.get("Name")).getValue();
				Map<String, String> properties;
				if(palleteRoot.containsKey("Properties")) {
					properties = new LinkedHashMap<>();
					CompoundMap map = ((CompoundTag)palleteRoot.get("Properties")).getValue();
					Iterator<Map.Entry<String, Tag<?>>> iterator = map.entrySet().iterator();
					while(iterator.hasNext()) {
						Map.Entry<String, Tag<?>> next = iterator.next();
						properties.put(next.getKey(), ((StringTag)next.getValue()).getValue());
					}
				} else {
					properties = Collections.emptyMap();
				}
				parsedPallete[i] = this.textures.get(blockId, properties);
			}
			int y = ((ByteTag)root.get("Y")).getValue();
			maxY = Math.max(maxY, y * 16 + 15);
			chunkSections[y] = new ChunkSection(
				blockStates.length * 64 / (16 * 16 * 16),
				blockStates,
				parsedPallete,
				false
			);
		}
		//System.out.println("CHunk at " + localX + "," + localZ + " has max height " + maxY);
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				int y = maxY;
				for(;y > 0; y--) {
					int sectionId = y / 16;
					int blockY = y % 16;
					ChunkSection s = chunkSections[sectionId];
					if(s.isEmpty()) {
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z);
					//System.out.println("Block at " + x + ',' + y + ',' + z + " is " + block.getBlock() + " and is " + block.isOpaque());
					if(block.isOpaque()) {
						break;
					}
				}
				//System.out.println("Blockcolumn at " + x + "," + z + " has min height " + y);
				for(;y <= maxY; y++) {
					int sectionId = y / 16;
					int blockY = y % 16;
					ChunkSection s = chunkSections[sectionId];
					if(s.isEmpty()) {
						continue;
					}
					TextureMapper block = s.getBlock(x, blockY, z);
					block.apply(g2, x + localX * 16, z + localZ * 16);
				}
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
			if(blockStates.length * 64 != blockPartSize * 16 * 16 * 16) {
				throw new IllegalArgumentException("blockStates length does not match expected: " + blockStates.length * 64 + " vs " + blockPartSize * 16 * 16 * 16);
			}
			this.blockPartSize = blockPartSize;
			this.blockMask = (1l << blockPartSize) - 1;
			this.blockStates = blockStates;
			this.states = states;
			this.empty = empty;
		}

		public TextureMapper getBlock(int x, int y, int z) {
			Preconditions.checkElementIndex(x, 16, "x");
			Preconditions.checkElementIndex(y, 16, "y");
			Preconditions.checkElementIndex(z, 16, "z");
			int blockPos = y*16*16 + z*16 + x;
			int bitOffset = blockPos* this.blockPartSize;
			int arrayIndex = bitOffset / 64;
			long arrayValue = this.blockStates[arrayIndex];
			int shiftOffset = bitOffset - (arrayIndex * 64);
			long movedValue = arrayValue >>> shiftOffset;
			long maskedValue = movedValue & this.blockMask;
			if(shiftOffset + this.blockPartSize > 64) {
				// We overflown during the shifting, TODO code this
				System.out.println("We overflown during the shifting, TODO code this: " + x + "," + y + "," + z);
			}
			if(maskedValue >= states.length) {
				throw new IllegalArgumentException("Calculated block id is larger than the pallete allows: " + maskedValue);
			}
			if(maskedValue < 0) {
				throw new IllegalArgumentException("Calculated block id is smaller than the pallete allows: " + maskedValue);
			}
			TextureMapper texture = this.states[(int)maskedValue];
			if(texture == null) {
				throw new IllegalArgumentException("Block " + maskedValue + " is null in the pallete");
			}
			return texture;
		}

		public boolean isEmpty() {
			return empty;
		}


	}
}
