/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import me.ferrybig.java.minecraft.overview.mapper.input.InputInfo;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;
import me.ferrybig.java.minecraft.overview.mapper.render.ImageWriter;

public class SequentialEngine {

	private static final Comparator<WorldFile> FILE_COMPARATOR = (WorldFile o1, WorldFile o2) -> {
		int compare = o1.getType().compareTo(o2.getType());
		if (compare != 0) {
			return compare;
		}
		assert o1.getType() == o2.getType();
		switch (o1.getType()) {
			case REGION_MCA:
				return Integer.compare(
					Math.abs(o1.getX()) + Math.abs(o1.getZ()),
					Math.abs(o2.getX()) + Math.abs(o2.getZ())
				);
			default:
				return 0; // No specific order
		}
	};

	private final InputSource files;
	private final RegionRenderer renderer;
	private final ImageWriter imageWriter;

	public SequentialEngine(InputSource files, RegionRenderer renderer, ImageWriter imageWriter) {
		this.files = files;
		this.renderer = renderer;
		this.imageWriter = imageWriter;
	}

	private boolean canConsume(WorldFile file) {
		switch (file.getType()) {
			case REGION_MCA:
				return file.getDimension() == 0;
			case LEVEL_DAT:
				return true;
			default:
				return false;
		}
	}

	public void render(BiConsumer<String, Double> beforeFile, BiConsumer<String, Double> afterFile) throws IOException {

		try (InputInfo inputTask = this.files.generateFileListing()) {
			System.out.println("Render start!");
			imageWriter.startRender();

			final Set<WorldFile> processedFiles;
			final int totalFiles;
			final boolean hasSendFullFileList;
			{
				Collection<WorldFile> fileNames = inputTask.getKnownFiles();
				if (fileNames.isEmpty()) {
					processedFiles = new HashSet<>();
					System.out.println("Input files not known");
					totalFiles = -1;
					hasSendFullFileList = false;
				} else {
					System.out.println("We have a full list of files! " + fileNames.size());
					imageWriter.addKnownFiles(fileNames);
					processedFiles = fileNames.stream().filter(this::canConsume).collect(Collectors.toSet());
					totalFiles = fileNames.size();
					hasSendFullFileList = true;
					for (Runnable run : imageWriter.filesKnown()) {
						run.run();
					}
				}
			}

			inputTask.forAllFiles(new InputInfo.FileConsumer() {
				private int processedFilesCount = 0;

				@Override
				public Comparator<WorldFile> getComparator() {
					return FILE_COMPARATOR;
				}

				@Override
				public boolean canConsume(WorldFile file) {
					return SequentialEngine.this.canConsume(file);
				}

				@Override
				public void consume(PreparedFile file) throws IOException {
					if (!hasSendFullFileList) {
						imageWriter.addKnownFile(file.getFile());
						processedFiles.add(file.getFile());
					} else {
						assert processedFiles.contains(file.getFile());
					}
					beforeFile.accept(file.getFile().getOrignalName(), processedFilesCount * 100d / totalFiles);
					switch (file.getFile().getType()) {
						case REGION_MCA: {
							BufferedImage render = SequentialEngine.this.renderer.renderFile(file);
							SequentialEngine.this.imageWriter.addFile(file.getFile(), render);
						}
						break;
						default: {
							SequentialEngine.this.imageWriter.addFile(file.getFile(), file);
						}
					}
					processedFilesCount++;
					afterFile.accept(file.getFile().getOrignalName(), processedFilesCount * 100d / totalFiles);

				}
			});

			if (!hasSendFullFileList) {
				for (Runnable run : imageWriter.filesKnown()) {
					run.run();
				}
			}

			imageWriter.finishRender();
		} finally {
			imageWriter.close();
		}
	}
}
