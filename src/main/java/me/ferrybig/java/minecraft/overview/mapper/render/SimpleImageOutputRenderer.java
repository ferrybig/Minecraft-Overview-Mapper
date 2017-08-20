/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.jnbt.CompoundTag;

/**
 *
 * @author Fernando
 */
public class SimpleImageOutputRenderer extends SimpleRenderer {

	private final Path outputDir;
	private final Path images;

	public SimpleImageOutputRenderer(RegionRenderer renderer, Path outputDir) {
		super(renderer);
		this.outputDir = outputDir;
		this.images = outputDir.resolve("tiles");
	}

	private Path getImageLocation(int x, int z) throws IOException {
		int modX = x % 16;
		if (modX < 0) {
			modX += 16;
		}
		int modZ = z % 16;
		if (modZ < 0) {
			modZ += 16;
		}
		Path p = images
				.resolve(Integer.toHexString(modX))
				.resolve(Integer.toHexString(modZ))
				.resolve(Integer.toHexString(x / 16) + "_" + Integer.toHexString(z / 16) + ".gif");
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
		try (OutputStream out = Files.newOutputStream(getImageLocation(x, z))) {
			ImageIO.write(tile, "gif", out);
		}
	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {

	}

}
