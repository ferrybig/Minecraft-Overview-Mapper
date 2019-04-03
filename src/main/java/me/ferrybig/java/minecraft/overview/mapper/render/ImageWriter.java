package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;

public interface ImageWriter extends Closeable {

	public default void addKnownFiles(@Nonnull Collection<WorldFile> files) {
		for (WorldFile file : files) {
			this.addKnownFile(file);
		}
	}

	public void addKnownFile(@Nonnull WorldFile file);

	public void startRender() throws IOException;

	public void addFile(@Nonnull WorldFile file, Object prepareResult) throws IOException;

	public void addCachedFile(@Nonnull WorldFile file) throws IOException;

	/**
	 * All files are known now, return a list of tasks to process all other zoom
	 * levels
	 *
	 * @return
	 */
	public @Nonnull
	List<Runnable> filesKnown();

	public void finishRender() throws IOException;

	public boolean supportsCache();

	public Path cacheFile() throws IOException;

	public Path cacheBackupFile() throws IOException;

}
