/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;

public interface InputInfo extends Closeable {

	@Nonnull
	public Collection<WorldFile> getKnownFiles();

	public boolean supportsSingleFile();

	public void forSingleFile(@Nonnull WorldFile file, @Nonnull FileConsumer consumer) throws IOException, NBTException;

	public void forAllFiles(@Nonnull FileConsumer consumer) throws IOException, NBTException;

	public default boolean supportsParallelExecution() {
		return false;
	}

	public default void forAllFilesParalel(@Nonnull FileConsumer consumer, int maxTasks) throws IOException, NBTException, InterruptedException {
		this.forAllFiles(consumer);
	}

	public default void forAllFilesParalel(@Nonnull FileConsumer consumer, int maxTasks, @Nonnull ExecutorService executor) throws IOException, NBTException, InterruptedException {
		this.forAllFiles(consumer);
	}

	@FunctionalInterface
	public interface FileConsumer {

		@Nullable
		public static Comparator<WorldFile> NO_COMPARE = null;

		@Nullable
		public default Comparator<WorldFile> getComparator() {
			return NO_COMPARE;
		}

		public default boolean canConsume(@Nonnull WorldFile file) {
			return true;
		}

		public void consume(@Nonnull PreparedFile file) throws IOException, NBTException;
	}
}
