/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import me.ferrybig.java.minecraft.overview.mapper.engine.ParallelTaskRunner;

public abstract class SimpleInputInfo implements InputInfo {

	protected final Collection<WorldFile> files;
	protected static final ThreadFactory PARALEL_RENDER_FACTORY = NamedThreadFactory.lazyFactory("SimpleInputInfo.renderer");

	public SimpleInputInfo(Collection<WorldFile> files) {
		this.files = files;
	}

	protected abstract PreparedFile toPreparedFile(WorldFile file) throws IOException;

	@Override
	public void close() throws IOException {
	}

	@Nonnull
	protected Stream<WorldFile> getSortedList(@Nonnull FileConsumer consumer) {
		final Comparator<WorldFile> comparator = consumer.getComparator();
		Stream<WorldFile> stream = this.files.stream().filter(consumer::canConsume);
		if (comparator != null) {
			stream = stream.sorted(comparator);
		}
		return stream;
	}

	@Override
	public void forAllFiles(@Nonnull FileConsumer consumer) throws IOException, NBTException {
		Iterator<WorldFile> iterator = this.getSortedList(consumer).iterator();
		while (iterator.hasNext()) {
			consumer.consume(this.toPreparedFile(iterator.next()));
		}
	}

	@Override
	public boolean supportsParallelExecution() {
		return true;
	}

	@Override
	public void forAllFilesParalel(FileConsumer consumer, int maxTasks) throws IOException, InterruptedException, NBTException {
		if (!this.supportsParallelExecution()) {
			InputInfo.super.forAllFilesParalel(consumer, maxTasks);
			return;
		}
		ExecutorService service = Executors.newFixedThreadPool(maxTasks, PARALEL_RENDER_FACTORY);
		try {
			try {
				this.forAllFilesParalel(consumer, maxTasks, service);
			} finally {
				service.shutdown();
			}
		} catch (InterruptedException ex) {
			while (!service.isTerminated()) {
				try {
					service.awaitTermination(1, TimeUnit.SECONDS);
				} catch (InterruptedException ignore) {
				}
			}
			throw ex;
		}
	}

	@Override
	public void forAllFilesParalel(FileConsumer consumer, int maxTasks, ExecutorService executor) throws IOException, InterruptedException, NBTException {
		if (!this.supportsParallelExecution()) {
			InputInfo.super.forAllFilesParalel(consumer, maxTasks, executor);
			return;
		}
		try {
			WorldFile[] worldFiles = this.getSortedList(consumer).toArray(WorldFile[]::new);
			new ParallelTaskRunner<>(executor, maxTasks, worldFiles, w -> consumer.consume(toPreparedFile(w))).start().waitForResult();
		} catch (ExecutionException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof IOException) {
				for (Throwable suppressed : ex.getSuppressed()) {
					cause.addSuppressed(suppressed);
				}
				throw (IOException) cause;
			} else if (cause instanceof NBTException) {
				for (Throwable suppressed : ex.getSuppressed()) {
					cause.addSuppressed(suppressed);
				}
				throw (NBTException) cause;
			} else {
				throw new RuntimeException(ex);
			}
		}
	}

	@Override
	public void forSingleFile(@Nonnull WorldFile file, @Nonnull FileConsumer consumer) throws IOException, NBTException {
		consumer.consume(this.toPreparedFile(file));
	}

	@Override
	public Collection<WorldFile> getKnownFiles() {
		return this.files;
	}

	@Override
	public boolean supportsSingleFile() {
		return true;
	}

}
