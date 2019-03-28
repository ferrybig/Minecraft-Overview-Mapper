/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ParallelTaskRunner<T> {

	@Nonnull
	private final AtomicInteger fileIndex = new AtomicInteger();
	@Nonnull
	private final ExecutorService pool;
	private final int maxTasks;
	private final int totalTasks;
	private int doneWorkers = 0;
	private volatile boolean isCancelled = false;
	@Nonnull
	private final List<Throwable> exceptions = new CopyOnWriteArrayList<>();
	@Nonnull
	private final IntFunction<T> getTask;
	@Nonnull
	private final ExceptionConsumer<T> runTask;

	public ParallelTaskRunner(
		@Nonnull ExecutorService pool,
		@Nonnegative int maxTasks,
		@Nonnull List<T> tasks,
		@Nonnull ExceptionConsumer<T> runTask
	) {
		this(pool, maxTasks, tasks::size, tasks::get, runTask);
	}

	public ParallelTaskRunner(
		@Nonnull ExecutorService pool,
		@Nonnegative int maxTasks,
		@Nonnull T[] tasks,
		@Nonnull ExceptionConsumer<T> runTask
	) {
		this(pool, maxTasks, () -> tasks.length, index -> tasks[index], runTask);
	}

	public ParallelTaskRunner(
		@Nonnull ExecutorService pool,
		@Nonnegative int maxTasks,
		@Nonnull IntSupplier totalTasks,
		@Nonnull IntFunction<T> getTask,
		@Nonnull ExceptionConsumer<T> runTask
	) {
		this.pool = Objects.requireNonNull(pool, "pool");
		this.totalTasks = totalTasks.getAsInt();
		this.maxTasks = Math.max(1, Math.min(maxTasks, this.totalTasks));
		this.getTask = Objects.requireNonNull(getTask, "getTask");
		this.runTask = Objects.requireNonNull(runTask, "runTask");
	}

	private T newTask() throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		int index = this.fileIndex.getAndIncrement();
		if (index < this.totalTasks) {
			return this.getTask.apply(index);
		}
		return null;
	}

	private void onWorkerDone(@Nullable Throwable ex) {
		synchronized (this) {
			if (ex != null) {
				this.exceptions.add(ex);
				this.isCancelled = true;
			}
			this.doneWorkers++;
			if (this.doneWorkers == this.maxTasks) {
				this.notifyAll();
			}
		}
	}

	private void taskRunner() {
		@Nullable
		Throwable exception = null;
		try {
			T task;
			while ((task = newTask()) != null) {
				this.runTask.run(task);
				if (this.isCancelled) {
					break;
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (Throwable ex) {
			exception = ex;
		}
		onWorkerDone(exception);
	}

	@Nonnull
	public ParallelTaskRunner<T> start() {
		int toSpawn = this.maxTasks;
		for (int i = 0; i < toSpawn; i++) {
			this.pool.submit(this::taskRunner);
		}
		return this;
	}

	public void waitWithoutInterrupt() {
		synchronized (this) {
			boolean interrupted = false;
			while (this.doneWorkers != this.maxTasks) {
				try {
					this.wait();
				} catch (InterruptedException ex) {
					interrupted = true;
				}
			}
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private boolean isDone0() {
		return this.doneWorkers != this.maxTasks;
	}

	public boolean isDone() {
		synchronized (this) {
			return this.isDone0();
		}
	}

	private void getResult() throws CancellationException, ExecutionException {
		assert this.isDone0();
		int exceptionsSize = this.exceptions.size();
		if (exceptionsSize > 0) {
			ExecutionException ex = new ExecutionException(this.exceptions.get(0));
			for (int i = 1; i < exceptionsSize; i++) { // Start loop at 1 so we skip the first exception
				ex.addSuppressed(this.exceptions.get(i));
			}
			throw ex;
		}
		if (this.isCancelled) {
			throw new CancellationException();
		}
		if (this.fileIndex.get() < this.totalTasks) {
			throw new ExecutionException(
				new IllegalStateException("Pool didn't execute all tasks, " + this.fileIndex.get() + " executed, vs " + this.totalTasks + " total")
			);
		}
	}

	public void waitForResult() throws CancellationException, InterruptedException, ExecutionException {
		boolean interrupted = false;
		synchronized (this) {
			while (isDone0()) {
				try {
					this.wait();
				} catch (InterruptedException ex) {
					this.isCancelled = true;
					interrupted = true;
				}
			}
		}
		if (interrupted) {
			throw new InterruptedException();
		}
		this.getResult();
	}

	public static interface ExceptionConsumer<T> {

		public void run(T item) throws Exception;
	}
}
