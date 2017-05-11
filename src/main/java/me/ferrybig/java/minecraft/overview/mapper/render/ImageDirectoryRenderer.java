/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.CompoundTag;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class ImageDirectoryRenderer extends SimpleRenderer {

	private final Path target;
	private final String imageformat;

	public ImageDirectoryRenderer(RegionRenderer renderer, Path target) {
		this(renderer, target, "gif");
	}

	public ImageDirectoryRenderer(RegionRenderer renderer, Path target, String imageformat) {
		super(renderer);
		this.target = target;
		this.imageformat = imageformat;
	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(target.resolve("level.json"))) {
			writer.append("{\"name\":\"" + ((CompoundTag) level.getValue().get("Data")).getValue().get("LevelName") + "\"}");
		}
	}

	@Override
	protected void addImage(BufferedImage tile, int x, int z) throws IOException {
		final Path targetFile = target.resolve("images/scale0/" + (x & 0xf) + "/" + (z & 0xf) + "/" + x + "_" + z + "." + imageformat);
		targetFile.getParent().toFile().mkdirs();
		try (OutputStream out = Files.newOutputStream(targetFile)) {
			ImageIO.write(tile, imageformat, out);
		}
	}

	@Override
	public void startRender() throws IOException {
		if(!target.toFile().exists() && !target.toFile().mkdirs())
			throw new IOException("Target directory creation failed");
		// TODO
	}

	@Override
	public void finishRender() throws IOException {
		// TODO
	}

}
