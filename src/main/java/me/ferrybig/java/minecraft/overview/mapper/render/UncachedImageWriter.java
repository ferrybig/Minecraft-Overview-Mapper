/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.IOException;
import java.nio.file.Path;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;

public abstract class UncachedImageWriter implements ImageWriter {

	@Override
	public Path cacheBackupFile() throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Path cacheFile() throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addCachedFile(WorldFile file) throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean supportsCache() {
		return false;
	}

}
