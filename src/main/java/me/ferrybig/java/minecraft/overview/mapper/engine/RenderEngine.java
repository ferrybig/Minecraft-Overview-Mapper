/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import com.google.common.util.concurrent.UncheckedExecutionException;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.overview.mapper.input.InputInfo;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;
import me.ferrybig.java.minecraft.overview.mapper.render.ImageWriter;

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

	@Nonnull
	private final InputSource files;
	@Nonnull
	private final RegionRenderer renderer;
	@Nonnull
	private final ImageWriter imageWriter;
	@Nonnull
	private final ProgressReporter reporter;
	@Nullable
	private final ParallelOptions parallel;

	private RenderEngine(@Nonnull RenderOptions options, @Nullable ParallelOptions parallel) {
		Objects.requireNonNull(options, "options");
		this.files = options.getFiles();
		this.renderer = options.getRenderer();
		this.imageWriter = options.getImageWriter();
		this.reporter = options.getProgress();
		this.parallel = parallel;
	}

	public static RenderEngine sequential(@Nonnull RenderOptions options) {
		return new RenderEngine(options, null);
	}

	public static RenderEngine parellel(@Nonnull RenderOptions options, int maxTasks, ExecutorService pool) {
		return new RenderEngine(options, new ParallelOptions(false, maxTasks, pool));
	}

	public static RenderEngine parellel(@Nonnull RenderOptions options, int maxTasks) {
		return parellel(options, maxTasks, true);
	}

	public static RenderEngine parellel(@Nonnull RenderOptions options, int maxTasks, boolean lowPriority) {
		return parellel(options, maxTasks, new ThreadFactory() {

			private final ThreadFactory parent = Executors.defaultThreadFactory();
			private final AtomicInteger id = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = parent.newThread(r);
				int myId = id.getAndIncrement();
				thread.setName("Render-engine-" + myId);
				if (lowPriority) {
					thread.setPriority(Thread.NORM_PRIORITY - 1);
				}
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	public static RenderEngine parellel(@Nonnull RenderOptions options, int maxTasks, ThreadFactory factory) {
		ExecutorService service = Executors.newFixedThreadPool(maxTasks, factory);
		boolean success = false;
		try {
			RenderEngine engine = new RenderEngine(options, new ParallelOptions(true, maxTasks, service));
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
			if (parallel.isShouldShutdownPool()) {
				parallel.getPool().shutdown();
			}
		}
		imageWriter.close();
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

	public void render() throws IOException {
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
					processedFiles = fileNames.stream().filter(this::canConsume).collect(Collectors.toSet());
					imageWriter.addKnownFiles(processedFiles);
					totalFiles = processedFiles.size();
					hasSendFullFileList = true;
					for (Runnable run : imageWriter.filesKnown()) {
						run.run();
					}
				}
			}
			final InputInfo.FileConsumer infoConsumer = makeInfoConsumer(hasSendFullFileList, processedFiles, totalFiles);
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
				try {
					// paralell operation
					inputTask.forAllFilesParalel(infoConsumer, maxTasks, parallelOptions.getPool());
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new UncheckedExecutionException(ex);
				}

				if (!hasSendFullFileList) {
					List<Runnable> tasks = imageWriter.filesKnown();
					int size = tasks.size();
					if (size > maxTasks) {
						// Todo: outfactor this dublicated logic
						CountDownLatch latch = new CountDownLatch(maxTasks);
						AtomicInteger fileIndex = new AtomicInteger();
						Runnable task = new Runnable() {
							private void runItem(Runnable task) throws IOException {
								task.run();
							}

							private Runnable newTask() {
								if (Thread.currentThread().isInterrupted()) {
									return null;
								}
								int index = fileIndex.getAndIncrement();
								if (index < size) {
									return tasks.get(index);
								}
								return null;
							}

							@Override
							public void run() {
								Runnable task;
								try {
									while ((task = newTask()) != null) {
										runItem(task);
									}
								} catch (Throwable ex) {
									ex.printStackTrace();
								}
								latch.countDown();
							}
						};
						for (int i = 0; i < maxTasks; i++) {
							parallelOptions.getPool().submit(task);
						}
						try {
							latch.await();
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
							throw new UncheckedExecutionException(ex);
						}
					} else if (size > 0) {
						List<Future<?>> futures = new ArrayList<>(size);
						for (Runnable task : tasks) {
							futures.add(parallelOptions.getPool().submit(task));
						}
						assert futures.size() == size;
						for (Future<?> f : futures) {
							try {
								f.get();
							} catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
								throw new UncheckedExecutionException(ex);
							} catch (ExecutionException ex) {
								throw new UncheckedExecutionException(ex);
							}
						}
					}
				}
			}

			imageWriter.finishRender();
		}
	}

	public InputInfo.FileConsumer makeInfoConsumer(
		final boolean hasSendFullFileList,
		final Set<WorldFile> processedFiles,
		final int totalFiles
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
			public void consume(PreparedFile file) throws IOException {
				if (!hasSendFullFileList) {
					imageWriter.addKnownFile(file.getFile());
					processedFiles.add(file.getFile());
				} else {
					assert processedFiles.contains(file.getFile());
				}
				RenderEngine.this.reporter.onFileStart(file.getFile());
				switch (file.getFile().getType()) {
					case REGION_MCA: {
						BufferedImage render = RenderEngine.this.renderer.renderFile(file);
						RenderEngine.this.imageWriter.addFile(file.getFile(), render);
					}
					break;
					default: {
						RenderEngine.this.imageWriter.addFile(file.getFile(), file);
					}
				}
				int processed = processedFilesCount.incrementAndGet();
				if (hasSendFullFileList) {
					double progress = processed * 100d / totalFiles;
					RenderEngine.this.reporter.onProgress(progress, processed, totalFiles);
				}
				RenderEngine.this.reporter.onFileEnd(file.getFile());

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

		public boolean isShouldShutdownPool() {
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
