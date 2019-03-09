/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.PriorityQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import me.ferrybig.java.minecraft.overview.mapper.streams.ByteCountingDataInputStream;

public class ChunkReader implements Closeable {

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
	 * Amount of integers present in an "integer" section of an region file
	 * sector
	 */
	private static final int SECTOR_INTS = SECTOR_BYTES / 4;

	private final PriorityQueue<Integer> chunkIndexes;
	private final ByteCountingDataInputStream in;
	private byte[] dataCache = new byte[4 * KIBIBYTE];

	public ChunkReader(InputStream input) throws IOException {
		this.in = new ByteCountingDataInputStream(wrapWithBuffer(input));
		this.chunkIndexes = new PriorityQueue<>(Integer::compare);
		for (int k = 0; k < SECTOR_INTS; ++k) {
			int offset = in.readInt();
			if (offset != 0) {
				int sectorNumber = offset >> 8;
				this.chunkIndexes.add(sectorNumber * SECTOR_BYTES);
			}
		}
		assert this.in.getReadBytes() == SECTOR_BYTES;
		this.skipFully(in, SECTOR_BYTES); // Skip timestamp sector
		assert this.in.getReadBytes() == SECTOR_BYTES * 2;
	}

	@Override
	public void close() throws IOException {
		this.in.close();
	}

	public InputStream nextChunk() throws IOException {
		Integer lowestChunkIndex = chunkIndexes.poll();
		if (lowestChunkIndex == null) {
			return null;
		}
		long chunkIndex = lowestChunkIndex;
		long bytesRead = in.getReadBytes();
		if (bytesRead > chunkIndex) {
			throw new IOException("We already read " + bytesRead + " but a chunk exists at byte index " + chunkIndex);
		} else if (bytesRead < chunkIndex) {
			this.skipFully(in, chunkIndex - bytesRead);
		}
		assert in.getReadBytes() == chunkIndex;
		int chunkLength = in.readInt();
		if (dataCache.length < chunkLength) {
			dataCache = new byte[(chunkLength / KIBIBYTE + 1) * KIBIBYTE];
		}
		in.readFully(dataCache, 0, chunkLength);
		byte version = dataCache[0];
		// Don't close chunkstream as its based on a ByteArrayInputStream object
		InputStream chunkStream = new ByteArrayInputStream(dataCache, 1, dataCache.length - 1);
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
		return chunkStream;
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

	private static InputStream wrapWithBuffer(InputStream input) {
		if (input instanceof BufferedInputStream || input instanceof ByteArrayInputStream) {
			return input;
		}
		return new BufferedInputStream(input, SECTOR_BYTES * 2);
	}

}
