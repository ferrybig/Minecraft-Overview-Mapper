/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.GZIPInputStream;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

public class ArchieveInputSource implements InputSource {

	public final File file;
	private final String subDirectory;

	public ArchieveInputSource(File file) {
		this(file, "");
	}

	public ArchieveInputSource(File file, String subDirectory) {
		this.file = file;
		this.subDirectory = subDirectory;
	}
//
//	@Override
//	public Stream<PreparedFile> stream() throws IOException {
//		TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(file)));
//		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<PreparedFile>() {
//
//			private TarArchiveEntry currentEntry;
//
//			@Override
//			public boolean hasNext() {
//				if (currentEntry != null) {
//					return true;
//				}
//				do {
//					try {
//						currentEntry = tarInput.getNextTarEntry();
//					} catch (IOException ex) {
//						throw new UncheckedIOException(ex);
//					}
//				} while (currentEntry != null && !currentEntry.getName().endsWith(".mca") && !currentEntry.getName().endsWith("level.dat"));
//				return currentEntry != null;
//			}
//
//			@Override
//			public PreparedFile next() {
//				if (!hasNext()) {
//					throw new NoSuchElementException();
//				}
//				final PreparedFile file = PreparedFile.of(currentEntry.getName(), () -> tarInput);
//				currentEntry = null;
//				return file;
//			}
//		}, 0), false).onClose(() -> {
//			try {
//				tarInput.close();
//			} catch (IOException ex) {
//				throw new UncheckedIOException(ex);
//			}
//		});
//	}
//
//	@Override
//	public void closeStreamIfNeeded(InputStream in) throws IOException {
//	}
//
//	@Override
//	public int totalFiles() {
//		return -1;
//	}

	@Override
	public InputInfo generateFileListing() throws IOException {
		boolean needToClose = true;
		FileInputStream fos = new FileInputStream(file);
		ArchiveInputStream inWrapper;
		try {
			String baseName = file.getName();
			if (baseName.endsWith(".tar.gz")) {
				inWrapper = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR, new GZIPInputStream(fos));
			} else if (baseName.endsWith(".tar")) {
				inWrapper = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR, fos);
			} else if (baseName.endsWith(".zip")) {
				inWrapper = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, fos);
			} else if (baseName.endsWith(".7z")) {
				inWrapper = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.SEVEN_Z, fos);
			} else {
				throw new IOException("Unsupported archieve type! " + baseName);
			}
			needToClose = false;
		} catch (ArchiveException ex) {
			throw new IOException(ex);
		} finally {
			if (needToClose) {
				fos.close();
			}
		}
		ArchiveInputStream in = inWrapper;
		return new InputInfo() {
			@Override
			public void close() throws IOException {
				in.close();
			}

			@Override
			public void forAllFiles(InputInfo.FileConsumer consumer) throws IOException, NBTException {
				ArchiveEntry nextEntry;
				while ((nextEntry = in.getNextEntry()) != null) {
					String fileName = nextEntry.getName();
					if (!subDirectory.isEmpty()) {
						if (!fileName.startsWith(subDirectory)) {
							continue;
						}
						fileName = fileName.substring(subDirectory.length() + 1);
					}
					WorldFile file = WorldFile.of(fileName);
					if (!consumer.canConsume(file)) {
						continue;
					}
					consumer.consume(PreparedFile.of(file, in));
				}
			}

			@Override
			public void forSingleFile(WorldFile file, InputInfo.FileConsumer consumer) throws IOException {
				throw new UnsupportedOperationException("Not supported.");
			}

			@Override
			public Collection<WorldFile> getKnownFiles() {
				return Collections.emptyList();
			}

			@Override
			public boolean supportsSingleFile() {
				return false;
			}
		};
	}

}
