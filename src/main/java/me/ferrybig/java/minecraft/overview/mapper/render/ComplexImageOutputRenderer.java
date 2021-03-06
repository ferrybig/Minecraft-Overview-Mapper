/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import me.ferrybig.java.minecraft.nbt.CompoundTag;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile.Type;

/**
 *
 * @author Fernando
 */
public class ComplexImageOutputRenderer extends SimpleRenderer {

	private static final boolean DEBUG = false;

	@Nonnull
	private final Path outputDir;
	@Nonnull
	private final Path images;
	private final int normalZoom = 10;
	private final int normalRes = 512;
	private final int halfRes = normalRes / 2;
	private final int maxZoom;
	private final int minZoom;
	@Nonnull
	private final String imageFormat = "png";
	@Nonnull
	private final String imageExtension = "png";

	@Nullable
	private final LayerConnector zoomLayer;

	public ComplexImageOutputRenderer(@Nonnull Path outputDir) {
		this(outputDir, 1, 14);
	}

	public ComplexImageOutputRenderer(@Nonnull Path outputDir, int minZoom, int maxZoom) {
		this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
		this.images = outputDir.resolve("complex-tiles");
		this.maxZoom = maxZoom;
		this.minZoom = minZoom;
		{
			LayerConnector zoomLayer = null;
			for (int currentZoom = minZoom; currentZoom < this.normalZoom; currentZoom++) {
				zoomLayer = new LayerConnector(zoomLayer, currentZoom, 0);
			}
			this.zoomLayer = zoomLayer;
		}
	}

	private Path getImageLocation(int dimension, int zoom, int x, int z) throws IOException {
		int modX = x % 16;
		if (modX < 0) {
			modX += 16;
		}
		int modZ = z % 16;
		if (modZ < 0) {
			modZ += 16;
		}
		Path p = images
			.resolve("DIM" + Integer.toString(dimension))
			.resolve(Integer.toString(zoom))
			.resolve(Integer.toString(x) + "_" + Integer.toString(z) + "." + imageExtension);
		Files.createDirectories(p.getParent());
		return p;
	}

	@Override
	public boolean supportsCache() {
		return true;
	}

	@Override
	public void addCachedFile(WorldFile file) throws IOException {
		if (this.zoomLayer != null) {
			this.zoomLayer.addCachedFile(file.getX(), file.getZ());
		}
	}

	@Override
	public Path cacheFile() throws IOException {
		return this.outputDir.resolve("render-cache.log");
	}

	@Override
	public Path cacheBackupFile() throws IOException {
		return this.outputDir.resolve("render-cache.bak.log");
	}

	@Override
	public void finishRender() throws IOException {
	}

	@Override
	public void startRender() throws IOException {
		Files.createDirectories(this.outputDir);
		Files.copy(
			this.getClass().getResourceAsStream("ComplexImageOutputRenderer.html"),
			this.outputDir.resolve("index.html"),
			StandardCopyOption.REPLACE_EXISTING
		);
	}

	@Override
	protected void addImage(BufferedImage tile, int dimension, int x, int z) throws IOException {
		int currentZoom = this.normalZoom;
		int imageWidth = tile.getWidth();
		int adjustedImageWidth = imageWidth;
		int tilesInThisLayer = 1;

		BufferedImage renderResult = new BufferedImage(this.normalRes, this.normalRes, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = renderResult.createGraphics();
		g2.setComposite(AlphaComposite.Src);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		try {
			for (; adjustedImageWidth >= this.normalRes; adjustedImageWidth /= 2, tilesInThisLayer *= 2, currentZoom++) {
				for (int tileX = 0; tileX < tilesInThisLayer; tileX++) {
					for (int tileZ = 0; tileZ < tilesInThisLayer; tileZ++) {
						g2.drawImage(
							tile,
							// dst x1,y1
							0, 0,
							// dst x2,y2
							this.normalRes, this.normalRes,
							// src x1,y1
							tileX * adjustedImageWidth, tileZ * adjustedImageWidth,
							// src x2,y2
							tileX * adjustedImageWidth + adjustedImageWidth, tileZ * adjustedImageWidth + adjustedImageWidth,
							null
						);
						final Path imageLocation = getImageLocation(
							dimension,
							currentZoom,
							x * tilesInThisLayer + tileX,
							z * tilesInThisLayer + tileZ
						);
						if (DEBUG && false) {
							System.err.println("Rendering zoom level " + currentZoom + ", tile: " + tileX + ", " + tileZ + " (" + x + "," + z + ") ");
							System.err.println(imageLocation);
						}
						ImageIO.write(renderResult, imageFormat, imageLocation.toFile());
					}
				}
				if (currentZoom >= this.maxZoom) {
					break;
				}
			}
			if (this.zoomLayer != null && dimension == 0) {
				this.zoomLayer.addRenderedFile(x, z);
			}
		} finally {
			g2.dispose();
		}

	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {

	}

	@Override
	public List<Runnable> filesKnown() {
		if (this.zoomLayer != null) {
			return this.zoomLayer.finalizeKnownFiles();
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public void addKnownFile(WorldFile file) {
		if (this.zoomLayer != null
			&& (file.getType() == Type.REGION_MCA || file.getType() == Type.REGION_MCR)
			&& file.getDimension() == 0) {
			this.zoomLayer.addKnownFile(file.getX(), file.getZ());
		}
	}

	@Override
	public String toString() {
		return "ComplexImageOutputRenderer{" + "outputDir=" + outputDir + ", minZoom=" + minZoom + ", maxZoom=" + maxZoom + ", imageExtension=" + imageExtension + '}';
	}

	private static class ImageEntry {

		private final int x;
		private final int z;

		public ImageEntry(int x, int z) {
			this.x = x;
			this.z = z;
		}

		public int getX() {
			return x;
		}

		public int getZ() {
			return z;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 43 * hash + this.x;
			hash = 43 * hash + this.z;
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
			final ImageEntry other = (ImageEntry) obj;
			if (this.x != other.x) {
				return false;
			}
			if (this.z != other.z) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "ImageEntry{" + "x=" + x + ", z=" + z + '}';
		}

	}
	private static final Function<ImageEntry, CoordinateData> COORDINATE_DATA_FACTORY = k -> new CoordinateData();

	private class LayerConnector {

		@Nonnull
		private final Map<ImageEntry, CoordinateData> images = new ConcurrentHashMap<>();
		private boolean allFilesKnown = false;
		@Nullable
		private final LayerConnector topLayer;
		private final int zoom;
		private final int dimension;

		public LayerConnector(LayerConnector topLayer, int zoom, int dimension) {
			this.topLayer = topLayer;
			this.zoom = zoom;
			this.dimension = dimension;
		}

		@Nonnull
		private ImageEntry computeEntry(int x, int z) {
			return new ImageEntry(Math.floorDiv(x, 2), Math.floorDiv(z, 2));
		}

		@Nonnull
		private CoordinateData getData(int x, int z) {
			return getData(computeEntry(x, z));
		}

		@Nonnull
		private CoordinateData getData(@Nonnull ImageEntry key) {
			return images.computeIfAbsent(key, COORDINATE_DATA_FACTORY);
		}

		public void addKnownFile(int x, int z) {
			ImageEntry key = computeEntry(x, z);
			CoordinateData data = getData(key);
			int oldKnown = data.known.getAndIncrement();
			if (oldKnown == 0 && topLayer != null) {
				topLayer.addKnownFile(key.getX(), key.getZ());
			}
		}

		public void addCachedFile(int x, int z) throws IOException {
			this.addFile(x, z, true);
		}

		public void addRenderedFile(int x, int z) throws IOException {
			this.addFile(x, z, false);
		}

		private void addFile(int x, int z, boolean cached) throws IOException {
			ImageEntry key = computeEntry(x, z);
			CoordinateData data = getData(key);
			int cachedValue;
			if (cached) {
				cachedValue = data.cached.incrementAndGet();
			} else {
				cachedValue = 0;
				// this is techinacally not correct, but if 1 file is not cached,
				// it really doesn't matter since we are going to render the tile now
			}
			int newRendered = data.rendered.incrementAndGet();
			int known = data.known.get();
			if (DEBUG) {
				System.out.println("Render status of zoom " + zoom + " tile " + key.getX() + "," + key.getZ() + ": " + newRendered + "/" + known);
			}
			if (allFilesKnown && newRendered == known) {
				renderTile(key.getX(), key.getZ(), known, cachedValue == newRendered);
			}
		}

		@Nonnull
		public List<Runnable> finalizeKnownFiles() {
			List<Runnable> tasks = new ArrayList<>();
			this.finalizeKnownFiles(tasks);
			return tasks;
		}

		private void finalizeKnownFiles(List<Runnable> tasks) {
			this.allFilesKnown = true;
			for (Map.Entry<ImageEntry, CoordinateData> entry : this.images.entrySet()) {
				CoordinateData data = entry.getValue();
				if (data.known.get() == data.rendered.get()) {
					ImageEntry key = entry.getKey();
					int tileX = key.getX();
					int tileZ = key.getZ();
					int known = data.known.get();
					int rendered = data.rendered.get();
					int cached = data.cached.get();
					boolean fullyCached = rendered == cached;
					tasks.add(() -> {
						try {
							renderTile(tileX, tileZ, known, fullyCached);
						} catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
				}
			}
			if (this.topLayer != null) {
				this.topLayer.finalizeKnownFiles(tasks);
			}
		}

		private void renderTile(int tileX, int tileZ, int known, boolean fullyCached) throws IOException {
			if (!fullyCached) {
				BufferedImage renderResult = new BufferedImage(normalRes, normalRes, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = renderResult.createGraphics();
				g2.setComposite(AlphaComposite.Src);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				try {
					int found = 0;
					for (int x = 0; x < 2; x++) {
						for (int z = 0; z < 2; z++) {
							try (InputStream in = Files.newInputStream(getImageLocation(
								dimension,
								zoom + 1,
								tileX * 2 + x,
								tileZ * 2 + z
							))) {
								final BufferedImage img = ImageIO.read(in);
								g2.drawImage(
									img,
									x * halfRes, z * halfRes,
									x * halfRes + halfRes, z * halfRes + halfRes,
									0, 0,
									normalRes, normalRes,
									null
								);
								found++;
							} catch (FileNotFoundException | NoSuchFileException ignore) {
							}
						}
					}
					assert found > 0 : "At least 1 image should exist in the layer, otherwise this entry should have never existed in this set";
					assert known >= found : "We rendered atleast " + known + " known tiles, we we only found " + found + " when rendering";
					final Path imageLocation = getImageLocation(
						dimension,
						zoom,
						tileX,
						tileZ
					);
					if (DEBUG) {
						System.err.println("Rendering zoom level " + zoom + ", tile: " + tileX + ", " + tileZ + " (" + found + "/" + known + ")");
						System.err.println(imageLocation);
					}
					ImageIO.write(renderResult, "gif", imageLocation.toFile());
				} finally {
					g2.dispose();
				}
			}
			if (this.topLayer != null) {
				if (fullyCached) {
					this.topLayer.addCachedFile(tileX, tileZ);
				} else {
					this.topLayer.addRenderedFile(tileX, tileZ);
				}
			}
		}

	}

	private static class CoordinateData {

		private final AtomicInteger rendered = new AtomicInteger();
		private final AtomicInteger known = new AtomicInteger();
		private final AtomicInteger cached = new AtomicInteger();

		@Override
		public String toString() {
			return "CoordinateData{" + "rendered=" + rendered + ", known=" + known + ", cached=" + cached + '}';
		}
	}

}
