/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class TarGzInputSource implements InputSource {

	public final File file;

	public TarGzInputSource(File file) {
		this.file = file;
	}

	@Override
	public Stream<PreparedFile> stream() throws IOException {
		TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(file)));
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<PreparedFile>() {

			private TarArchiveEntry currentEntry;

			@Override
			public boolean hasNext() {
				if (currentEntry != null) {
					return true;
				}
				do {
					try {
						currentEntry = tarInput.getNextTarEntry();
					} catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				} while (currentEntry != null && !currentEntry.getName().endsWith(".mca") && !currentEntry.getName().endsWith("level.dat"));
				return currentEntry != null;
			}

			@Override
			public PreparedFile next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				final PreparedFile file = PreparedFile.of(currentEntry.getName(), () -> tarInput);
				currentEntry = null;
				return file;
			}
		}, 0), false).onClose(() -> {
			try {
				tarInput.close();
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});
	}

	@Override
	public void closeStreamIfNeeded(InputStream in) throws IOException {
	}

}
