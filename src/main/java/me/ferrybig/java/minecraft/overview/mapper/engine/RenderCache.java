/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RenderCache implements Closeable {

	@Nonnull
	private final Map<String, CacheEntry> entries;
	@Nonnull
	private final BufferedWriter writer;
	private static final Version CURRENT_VERSION = Version.VERSION_1_1;

	public RenderCache(@Nonnull Path normalFile, @Nonnull Path backupFile) throws IOException {
		boolean normalExists = Files.exists(normalFile);
		boolean backupExists = Files.exists(backupFile);
		if (normalExists && !backupExists) {
			Files.move(normalFile, backupFile, ATOMIC_MOVE);
		}
		Map<String, CacheEntry> entries = new HashMap<>();
		boolean hasExistingData = normalExists || backupExists;
		if (hasExistingData) {
			// Existing files exists, and have been moved to the backup file
			try (BufferedReader reader = Files.newBufferedReader(backupFile, StandardCharsets.UTF_8)) {
				Version version = Version.VERSION_1_0;
				String nextLine;
				while ((nextLine = reader.readLine()) != null) {
					if (nextLine.startsWith("#")) {
						Version newVersion = Version.getVersion(nextLine.substring(1));
						if (newVersion != null) {
							version = newVersion;
						}
						continue;
					}
					CacheEntry entry = version.readLine(nextLine);
					if (entry != null) {
						entries.put(entry.filename, entry);
					}
				}
			}
			// Recreate normal file
			try (BufferedWriter writer = Files.newBufferedWriter(normalFile, StandardCharsets.UTF_8)) {
				writer.write(CURRENT_VERSION.asVersionString());
				for (CacheEntry entry : entries.values()) {
					writer.write(entry.toLine());
				}
			}
			Files.delete(backupFile);
		}
		this.entries = new ConcurrentHashMap<>(entries);
		Files.createDirectories(normalFile.getParent());
		this.writer = Files.newBufferedWriter(normalFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		if (!hasExistingData) {
			writer.write(CURRENT_VERSION.asVersionString());
		}
	}

	public int getLastModificationDate(@Nonnull String fileName) {
		CacheEntry entry = this.entries.get(fileName);
		if (entry == null) {
			return Integer.MIN_VALUE;
		}
		return entry.lastModification;
	}

	public void storeLastModificationDate(@Nonnull String fileName, int lastModification) throws IOException {
		final CacheEntry cacheEntry = new CacheEntry(fileName, lastModification, System.currentTimeMillis());
		this.entries.put(fileName, cacheEntry);
		synchronized (this) {
			this.writer.write(cacheEntry.toLine());
			this.writer.flush();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			this.writer.close();
		}
	}

	private static class CacheEntry {

		@Nonnull
		private final String filename;
		private final int lastModification;
		private final long lastRenderTime;

		public CacheEntry(@Nonnull String filename, int lastModification, long lastRenderTime) {
			this.filename = filename;
			this.lastModification = lastModification;
			this.lastRenderTime = lastRenderTime;
		}

		@Nullable
		public static CacheEntry readFromLineV1_0(@Nonnull String line) {
			String[] split = line.split("\t", 3);
			if (split.length != 3) {
				return null; // Corrupt entry
			}
			int lastModification = Integer.parseInt(split[0]);
			int fileNameLength = Integer.parseInt(split[1]);
			if (split[2].length() != fileNameLength) {
				return null; // Corrupt entry
			}
			return new CacheEntry(split[2], lastModification, 0);
		}

		@Nullable
		public static CacheEntry readFromLineV1_1(@Nonnull String line) {
			String[] split = line.split("\t", 4);
			if (split.length != 4) {
				return null; // Corrupt entry
			}
			int lastModification = Integer.parseInt(split[0]);
			long lastRenderTime = Long.parseLong(split[1]);
			int fileNameLength = Integer.parseInt(split[2]);
			if (split[3].length() != fileNameLength) {
				return null; // Corrupt entry
			}
			return new CacheEntry(split[3], lastModification, lastRenderTime);
		}

		@Nonnull
		public String toLine() {
			return lastModification + "\t" + lastRenderTime + "\t" + filename.length() + "\t" + filename + "\n";
		}
	}

	private static enum Version {
		VERSION_1_0("v1.0", CacheEntry::readFromLineV1_0),
		VERSION_1_1("v1.1", CacheEntry::readFromLineV1_1),;
		@Nonnull
		private final Function<String, CacheEntry> mapper;
		@Nonnull
		private final String versionString;

		private Version(String versionString, Function<String, CacheEntry> mapper) {
			this.versionString = versionString;
			this.mapper = mapper;
		}

		@Nullable
		public CacheEntry readLine(@Nonnull String line) {
			return this.mapper.apply(line);
		}

		public String asVersionString() {
			return "#" + this.versionString + "\n";
		}

		@Nullable
		public static Version getVersion(@Nonnull String name) {
			return VERSIONS.get(name);
		}

		@Nonnull
		private static final Map<String, Version> VERSIONS;

		static {
			Version[] values = values();
			Map<String, Version> versions = new HashMap<>(values.length);
			for (Version v : values) {
				versions.put(v.versionString, v);
			}
			VERSIONS = Collections.unmodifiableMap(versions);
		}
	}
}
