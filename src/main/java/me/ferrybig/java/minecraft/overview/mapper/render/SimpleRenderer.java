/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.CompoundTag;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;

public abstract class SimpleRenderer extends UncachedImageWriter {

	@Override
	public void addFile(WorldFile file, Object renderResult) throws IOException {
		switch (file.getType()) {
			case REGION_MCA:
			case REGION_MCR:
				this.addImage((BufferedImage) renderResult, file.getDimension(), file.getX(), file.getZ());
				break;
			default:
				System.out.println("Unknown file: " + file.getType() + ": " + file.getOrignalName());
		}
	}

	protected abstract void addLevelDat(CompoundTag level) throws IOException;

	protected abstract void addImage(BufferedImage tile, int dimension, int x, int z)
		throws IOException;

	@Override
	public void addKnownFile(WorldFile file) {
	}

	@Override
	public void startRender() throws IOException {
	}

	@Override
	public List<Runnable> filesKnown() {
		return Collections.emptyList();
	}

	@Override
	public void finishRender() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

}
