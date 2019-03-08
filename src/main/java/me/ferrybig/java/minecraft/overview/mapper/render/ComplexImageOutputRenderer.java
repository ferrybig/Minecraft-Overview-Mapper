/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.CompoundTag;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 *
 * @author Fernando
 */
public class ComplexImageOutputRenderer extends SimpleRenderer {

	private final Path outputDir;
	private final Path images;
	private final int normalZoom = 10;
	private final int normalRes = 256;
	private final int maxZoom = 15;
	private final Set<ImageEntry> zoomedOutLayers = new HashSet<>();

	public ComplexImageOutputRenderer(RegionRenderer renderer, Path outputDir) {
		super(renderer);
		this.outputDir = outputDir;
		this.images = outputDir.resolve("complex-tiles");
	}

	private Path getImageLocation(int zoom, int x, int z) throws IOException {
		int modX = x % 16;
		if (modX < 0) {
			modX += 16;
		}
		int modZ = z % 16;
		if (modZ < 0) {
			modZ += 16;
		}
		Path p = images
			.resolve(Integer.toString(zoom))
			.resolve(Integer.toString(x) + "_" + Integer.toString(z) + ".gif");
		Files.createDirectories(p.getParent());
		return p;
	}

	@Override
	public void finishRender() throws IOException {
		Set<ImageEntry> pendingTiles = this.zoomedOutLayers;
		final int halfRes = this.normalRes / 2;
		int zoomLevel = normalZoom - 1;
		BufferedImage renderResult = new BufferedImage(this.normalRes, this.normalRes, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2 = renderResult.createGraphics();
		g2.setComposite(AlphaComposite.Src);
		int[] emptyArray = renderResult.getRGB(0, 0, this.normalRes, this.normalRes, null, 0, this.normalRes);
		try {
			while (zoomLevel > 1) {
				Set<ImageEntry> newTiles = new HashSet<>();
				for (ImageEntry entry : pendingTiles) {
					renderResult.setRGB(0, 0, this.normalRes, this.normalRes, emptyArray, 0, this.normalRes);
					int found = 0;
					for (int x = 0; x < 2; x++) {
						for (int z = 0; z < 2; z++) {
							try (InputStream in = Files.newInputStream(getImageLocation(
								zoomLevel + 1,
								entry.getX() * 2 + x,
								entry.getZ() * 2 + z
							))) {
								final BufferedImage img = ImageIO.read(in);
								g2.drawImage(
									img,
									x * halfRes, z * halfRes,
									x * halfRes + halfRes, z * halfRes + halfRes,
									0, 0,
									this.normalRes, this.normalRes,
									null
								);
								found++;
							} catch (FileNotFoundException | NoSuchFileException ignore) {
							}
						}
					}
					assert found > 0 : "At least 1 image should exist in the layer, otherwise this entry should have never existed in this set";
					final Path imageLocation = getImageLocation(
						zoomLevel,
						entry.getX(),
						entry.getZ()
					);
					System.err.println("Rendering zoom level " + zoomLevel + ", tile: " + entry.getX() + ", " + entry.getZ() + " (" + found + "/4)");
					System.err.println(imageLocation);
					try (OutputStream out = Files.newOutputStream(imageLocation)) {
						ImageIO.write(renderResult, "gif", out);
					}
					newTiles.add(new ImageEntry(Math.floorDiv(entry.getX(), 2), Math.floorDiv(entry.getZ(), 2)));
				}
				pendingTiles = newTiles;
				zoomLevel -= 1;
			}
		} finally {
			g2.dispose();
		}
	}

	@Override
	public void startRender() throws IOException {
		Files.createDirectories(outputDir);
	}

	@Override
	protected void addImage(BufferedImage tile, int x, int z) throws IOException {
		int currentZoom = this.normalZoom;
		int imageWidth = tile.getWidth();
		int adjustedImageWidth = imageWidth;
		int tilesToGenerate = 1;

		BufferedImage renderResult = new BufferedImage(this.normalRes, this.normalRes, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2 = renderResult.createGraphics();
		g2.setComposite(AlphaComposite.Src);
		try {
			for (; adjustedImageWidth >= this.normalRes; adjustedImageWidth /= 2, tilesToGenerate *= 2, currentZoom++) {
				for (int tileX = 0; tileX < tilesToGenerate; tileX++) {
					for (int tileZ = 0; tileZ < tilesToGenerate; tileZ++) {
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
							currentZoom,
							x * tilesToGenerate + tileX,
							z * tilesToGenerate + tileZ
						);
						System.err.println("Rendering zoom level " + currentZoom + ", tile: " + tileX + ", " + tileZ + " (" + x + "," + z + ") ");
						System.err.println(imageLocation);
						try (OutputStream out = Files.newOutputStream(imageLocation)) {
							ImageIO.write(renderResult, "gif", out);
						}
						if (adjustedImageWidth == imageWidth) {
							// This image is part of the "main" layer, and will be used to generate the more zoomed out layer
							zoomedOutLayers.add(new ImageEntry(Math.floorDiv(x, 2), Math.floorDiv(z, 2)));
						}
					}
				}
				if (currentZoom >= this.maxZoom) {
					break;
				}
			}
		} finally {
			g2.dispose();
		}

	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {

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

}
