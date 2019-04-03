/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import me.ferrybig.java.minecraft.overview.mapper.input.InputInfo;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;
import me.ferrybig.java.minecraft.overview.mapper.render.ImageWriter;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RenderOutput;

public class RenderEngine implements Closeable {

	private static final Comparator<WorldFile> FILE_COMPARATOR = (WorldFile o1, WorldFile o2) -> {
		int compare = o1.getType().compareTo(o2.getType());
		if (compare != 0) {
			return compare;
		}
		assert o1.getType() == o2.getType();
		switch (o1.getType()) {
			case REGION_MCA:
				return Integer.compare(
					Math.max(Math.abs(o1.getX()), Math.abs(o1.getZ())),
					Math.max(Math.abs(o2.getX()), Math.abs(o2.getZ()))
				);
			default:
				return 0; // No specific order
		}
	};

	@Nullable
	private final ParallelOptions parallel;

	private RenderEngine(@Nullable ParallelOptions parallel) {
		this.parallel = parallel;
	}

	public static RenderEngine sequential() {
		return new RenderEngine(null);
	}

	public static RenderEngine parellel(int maxTasks, ExecutorService pool) {
		return new RenderEngine(new ParallelOptions(false, maxTasks, pool));
	}

	public static RenderEngine parellel(int maxTasks) {
		return parellel(maxTasks, Thread.MIN_PRIORITY);
	}

	public static RenderEngine parellel(int maxTasks, int priority) {
		return parellel(maxTasks, new ThreadFactory() {

			private final ThreadFactory parent = Executors.defaultThreadFactory();
			private final AtomicInteger id = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = parent.newThread(r);
				int myId = id.getAndIncrement();
				thread.setName("Render-engine-" + myId);
				if (priority != thread.getPriority()) {
					thread.setPriority(priority);
				}
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	public static RenderEngine parellel(int maxTasks, ThreadFactory factory) {
		ExecutorService service = Executors.newFixedThreadPool(maxTasks, factory);
		boolean success = false;
		try {
			RenderEngine engine = new RenderEngine(new ParallelOptions(true, maxTasks, service));
			success = true;
			return engine;
		} finally {
			if (!success) {
				service.shutdown();
			}
		}
	}

	@Override
	public void close() throws IOException {
		ParallelOptions parallel = this.parallel;
		if (parallel != null) {
			if (parallel.shouldShutdownPool()) {
				parallel.getPool().shutdown();
			}
		}
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

	@Nullable
	private Set<WorldFile> prepareJob(InputInfo task, ImageWriter imageWriter) {
		Collection<WorldFile> fileNames = task.getKnownFiles();
		if (fileNames.isEmpty()) {
			System.out.println("Input files not known");
			return null;
		} else {
			System.out.println("We have a full list of files! " + fileNames.size());
			Set<WorldFile> processedFiles = fileNames.stream().filter(this::canConsume).collect(Collectors.toSet());
			imageWriter.addKnownFiles(processedFiles);
			List<Runnable> filesKnown = imageWriter.filesKnown();
			for (Runnable run : filesKnown) {
				run.run();
			}
			return processedFiles;
		}
	}

	@Nullable
	private RenderCache makeCache(ImageWriter imageWriter) throws IOException {
		if (imageWriter.supportsCache()) {
			return new RenderCache(imageWriter.cacheFile(), imageWriter.cacheBackupFile());
		} else {
			return null;
		}
	}

	public void runJob(RenderOptions options) throws RenderException, InterruptedException, CancellationException {
		InputSource files = options.getFiles();
		RegionRenderer renderer = options.getRenderer();
		ImageWriter imageWriter = options.getImageWriter();
		ProgressReporter reporter = options.getProgress();

		try (InputInfo inputTask = files.generateFileListing(); RenderCache cache = makeCache(imageWriter)) {
			System.out.println("Render start!");
			imageWriter.startRender();

			final Set<WorldFile> processedFiles;
			final int totalFiles;
			final boolean hasSendFullFileList;
			{
				Set<WorldFile> preparedJob = prepareJob(inputTask, imageWriter);
				if (preparedJob == null) {
					processedFiles = new HashSet<>();
					totalFiles = -1;
					hasSendFullFileList = false;
				} else {
					processedFiles = preparedJob;
					totalFiles = processedFiles.size();
					hasSendFullFileList = true;
				}
			}
			final InputInfo.FileConsumer infoConsumer = makeInfoConsumer(
				hasSendFullFileList, processedFiles, totalFiles, cache, renderer, imageWriter, reporter
			);
			final ParallelOptions parallelOptions = this.parallel;

			if (parallelOptions == null) {
				// sequential operation
				inputTask.forAllFiles(infoConsumer);
				if (!hasSendFullFileList) {
					for (Runnable run : imageWriter.filesKnown()) {
						run.run();
					}
				}
			} else {
				final int maxTasks = parallelOptions.getMaxTasks();
				inputTask.forAllFilesParalel(infoConsumer, maxTasks, parallelOptions.getPool());

				if (!hasSendFullFileList) {
					List<Runnable> tasks = imageWriter.filesKnown();
					new ParallelTaskRunner<>(parallelOptions.getPool(), parallelOptions.getMaxTasks(), tasks, Runnable::run)
						.start()
						.waitForResult();
				}
			}

			imageWriter.finishRender();
		} catch (IOException | NBTException | ExecutionException ex) {
			throw new RenderException(ex);
		}
	}

	@Nonnull
	private InputInfo.FileConsumer makeInfoConsumer(
		final boolean hasSendFullFileList,
		final Set<WorldFile> processedFiles,
		final int totalFiles,
		final RenderCache cache,
		RegionRenderer renderer,
		ImageWriter imageWriter,
		ProgressReporter reporter
	) {
		return new InputInfo.FileConsumer() {
			private final AtomicInteger processedFilesCount = new AtomicInteger();

			@Override
			public Comparator<WorldFile> getComparator() {
				return RenderEngine.FILE_COMPARATOR;
			}

			@Override
			public boolean canConsume(WorldFile file) {
				return RenderEngine.this.canConsume(file);
			}

			@Override
			public void consume(PreparedFile prepared) throws IOException, NBTException {
				final WorldFile file = prepared.getFile();
				if (!hasSendFullFileList) {
					imageWriter.addKnownFile(file);
					processedFiles.add(file);
				} else {
					assert processedFiles.contains(file);
				}
				reporter.onFileStart(file);
				switch (file.getType()) {
					case REGION_MCA: {
						final String orignalName = file.getOrignalName();
						int lastModification = cache.getLastModificationDate(orignalName);
						RenderOutput render = renderer.renderFile(prepared, lastModification);
						if (render.getOutput() != null) {
							imageWriter.addFile(file, render.getOutput());
							cache.storeLastModificationDate(orignalName, render.getLastModification());
						} else {
							System.out.println("Using file from cache: " + orignalName);
							imageWriter.addCachedFile(file);
						}
					}
					break;
					default: {
						imageWriter.addFile(file, prepared);
					}
				}
				int processed = processedFilesCount.incrementAndGet();
				if (hasSendFullFileList) {
					double progress = processed * 100d / totalFiles;
					reporter.onProgress(progress, processed, totalFiles);
				}
				reporter.onFileEnd(file);

			}
		};
	}

	private static class ParallelOptions {

		private final boolean shouldShutdownPool;
		private final int maxTasks;
		@Nonnull
		private final ExecutorService pool;

		public ParallelOptions(boolean shouldShutdownPool, int maxTasks, ExecutorService pool) {
			this.shouldShutdownPool = shouldShutdownPool;
			this.maxTasks = maxTasks;
			this.pool = pool;
		}

		public boolean shouldShutdownPool() {
			return shouldShutdownPool;
		}

		public int getMaxTasks() {
			return maxTasks;
		}

		public ExecutorService getPool() {
			return pool;
		}

		@Override
		public String toString() {
			return "ParallelOptions{" + "shouldShutdownPool=" + shouldShutdownPool + ", maxTasks=" + maxTasks + ", pool=" + pool + '}';
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 17 * hash + (this.shouldShutdownPool ? 1 : 0);
			hash = 17 * hash + this.maxTasks;
			hash = 17 * hash + Objects.hashCode(this.pool);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ParallelOptions other = (ParallelOptions) obj;
			if (this.shouldShutdownPool != other.shouldShutdownPool) {
				return false;
			}
			if (this.maxTasks != other.maxTasks) {
				return false;
			}
			if (!Objects.equals(this.pool, other.pool)) {
				return false;
			}
			return true;
		}

	}
}
