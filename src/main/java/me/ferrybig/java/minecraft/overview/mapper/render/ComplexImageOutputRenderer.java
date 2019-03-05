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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
			for(;adjustedImageWidth >= this.normalRes; adjustedImageWidth /= 2, tilesToGenerate *= 2, currentZoom++) {
				for(int tileX = 0; tileX < tilesToGenerate; tileX++) {
					for(int tileZ = 0; tileZ < tilesToGenerate; tileZ++) {
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
					}
				}
			}
		} finally {
			g2.dispose();
		}
		
	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {

	}

}
