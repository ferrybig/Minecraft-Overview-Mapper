/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.render.ChunkReader;

public abstract class PreparedFile implements Closeable {

	@Nonnull
	private final WorldFile file;

	public PreparedFile(@Nonnull WorldFile file) {
		this.file = Objects.requireNonNull(file, "file");
	}

	@Nonnull
	public WorldFile getFile() {
		return file;
	}

	@Nonnull
	public abstract InputStream getInputstream();

	@Nonnull
	public ChunkReader getChunkReader() throws IOException {
		return new ChunkReader(this.getInputstream());
	}

	@Nonnull
	public static PreparedFile of(@Nonnull WorldFile name, @Nonnull File file) throws FileNotFoundException, IOException {
		return of(name, new FileInputStream(file));
	}

	@Nonnull
	public static PreparedFile of(@Nonnull WorldFile name, @Nonnull Path file) throws FileNotFoundException, IOException {
		return of(name, new FileInputStream(file.toFile()));
	}

	@Nonnull
	public static PreparedFile of(@Nonnull WorldFile name, @Nonnull InputStream in) {
		return new PreparedFile(name) {
			@Override
			public void close() throws IOException {
				in.close();
			}

			@Override
			public InputStream getInputstream() {
				return in;
			}

		};
	}

	@Nonnull
	public static PreparedFile of(@Nonnull WorldFile name, @Nonnull byte[] data) {
		return of(name, data, 0, data.length);
	}

	@Nonnull
	public static PreparedFile of(@Nonnull WorldFile name, @Nonnull byte[] data, int offset, int length) {
		ByteArrayInputStream in = new ByteArrayInputStream(data, offset, length);

		return new PreparedFile(name) {
			@Override
			public void close() throws IOException {
			}

			@Override
			public InputStream getInputstream() {
				return in;
			}

		};
	}

}
