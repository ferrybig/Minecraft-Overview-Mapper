/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap.Biome;
import me.ferrybig.java.minecraft.overview.mapper.render.BlockMap.Block;
import me.ferrybig.java.minecraft.overview.mapper.streams.ByteCountingDataInputStream;

/**
 * This is the default image renderer used by the code, this render as a nice
 * overview map
 *
 * @author Fernando
 */
public class DefaultImageRenderer implements RegionRenderer {

	private static final int VERSION_GZIP = 1;
	private static final int VERSION_DEFLATE = 2;

	private static final int SECTOR_BYTES = 4096;
	private static final int SECTOR_INTS = SECTOR_BYTES / 4;

	private static final int SHADE_CUTOFF = 32;
	private final static int MAX_CHUNK_SECTIONS = 16;

	private final BlockMap blockMap;
	private final BiomeMap biomeMap;

	public DefaultImageRenderer(BlockMap blockMap, BiomeMap biomeMap) {
		this.blockMap = blockMap;
		this.biomeMap = biomeMap;
	}

	@Override
	public BufferedImage renderFile(PreparedFile file) throws IOException {
		ByteCountingDataInputStream in = new ByteCountingDataInputStream(new BufferedInputStream(file.getInputstream(), SECTOR_BYTES * 2));
		PriorityQueue<Integer> chunkIndexes = new PriorityQueue<>(
			new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(o1, o2);
			}
		}
		);
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
		Integer lowestChunkIndex;
		CompoundTag globalTag;
		CompoundTag levelTag;
		byte[] data = new byte[4 * KIBIBYTE];
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

			loadChunkData(levelTag,
				blockId, blockData, sectionsUsedList, biomes);
			preRenderChunk(
				blockId, blockData, sectionsUsedList, biomes, blockMap,
				biomeMap, regionX, regionZ, imageColorArray, imageShadeArray);
		}
		demultiplyAlpha(imageColorArray);
		shade(imageShadeArray, imageColorArray);

		BufferedImage localBufferedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		for (int k = 0; k < 512; k++) {
			localBufferedImage.setRGB(0, k, 512, 1, imageColorArray, 512 * k, 512);
		}
		return localBufferedImage;
	}
	private static final int KIBIBYTE = 1024;

	@SuppressWarnings("unchecked")
	private static void loadChunkData(CompoundTag levelTag, short[][] sectionBlockIds, byte[][] sectionBlockData, boolean[] sectionsUsed, byte[] biomeIds) {
		for (int i = 0; i < MAX_CHUNK_SECTIONS; i++) {
			sectionsUsed[i] = false;
		}
		Tag<?> biomesTag = levelTag.getValue().get("Biomes");
		if (biomesTag != null) {
			System.arraycopy(((ByteArrayTag) biomesTag).getValue(), 0, biomeIds, 0, 16 * 16);
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
		int i = 512;
		int j = 512;

		int k = 0;
		for (int m = 0; m < j; m++) {
			for (int n = 0; n < i; k++) {
				if (imageShadeArray[k] != 0) {
					float f1;
					if (n == 0) {
						f1 = imageColorArray[(k + 1)] - imageColorArray[k];
					} else if (n == i - 1) {
						f1 = imageColorArray[k] - imageColorArray[(k - 1)];
					} else {
						f1 = (imageColorArray[(k + 1)] - imageColorArray[(k - 1)]) * 2;
					}
					float f2;
					if (m == 0) {
						f2 = imageColorArray[(k + i)] - imageColorArray[k];
					} else if (m == j - 1) {
						f2 = imageColorArray[k] - imageColorArray[(k - i)];
					} else {
						f2 = (imageColorArray[(k + i)] - imageColorArray[(k - i)]) * 2;
					}
					float f3 = f1 + f2;
					if (f3 > 10.0F) {
						f3 = 10.0F;
					}
					if (f3 < -10.0F) {
						f3 = -10.0F;
					}

					f3 = (float) (f3 + (imageColorArray[k] - 64) / 7.0D);

					imageShadeArray[k] = Color.shade(imageShadeArray[k], (int) (f3 * 8.0F));
				}
				n++;
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
}
