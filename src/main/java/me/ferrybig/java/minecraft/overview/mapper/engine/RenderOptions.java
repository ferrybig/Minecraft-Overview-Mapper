/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import java.util.Objects;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.render.ImageWriter;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;

public final class RenderOptions {

	@Nonnull
	private final InputSource files;
	@Nonnull
	private final RegionRenderer renderer;
	@Nonnull
	private final ImageWriter imageWriter;
	@Nonnull
	private final ProgressReporter progress;

	public RenderOptions(
		@Nonnull InputSource files,
		@Nonnull RegionRenderer renderer,
		@Nonnull ImageWriter imageWriter,
		@Nonnull ProgressReporter progress
	) {
		this.files = Objects.requireNonNull(files, "files");
		this.renderer = Objects.requireNonNull(renderer, "renderer");
		this.imageWriter = Objects.requireNonNull(imageWriter, "imageWriter");
		this.progress = Objects.requireNonNull(progress, "progress");
	}

	@Nonnull
	public InputSource getFiles() {
		return files;
	}

	@Nonnull
	public RegionRenderer getRenderer() {
		return renderer;
	}

	@Nonnull
	public ImageWriter getImageWriter() {
		return imageWriter;
	}

	@Nonnull
	public ProgressReporter getProgress() {
		return progress;
	}

	@Override
	public String toString() {
		return "RenderOptions{" + "files=" + files + ", renderer=" + renderer + ", imageWriter=" + imageWriter + ", progress=" + progress + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(this.files);
		hash = 97 * hash + Objects.hashCode(this.renderer);
		hash = 97 * hash + Objects.hashCode(this.imageWriter);
		hash = 97 * hash + Objects.hashCode(this.progress);
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
		final RenderOptions other = (RenderOptions) obj;
		if (!Objects.equals(this.files, other.files)) {
			return false;
		}
		if (!Objects.equals(this.renderer, other.renderer)) {
			return false;
		}
		if (!Objects.equals(this.imageWriter, other.imageWriter)) {
			return false;
		}
		if (!Objects.equals(this.progress, other.progress)) {
			return false;
		}
		return true;
	}

}
