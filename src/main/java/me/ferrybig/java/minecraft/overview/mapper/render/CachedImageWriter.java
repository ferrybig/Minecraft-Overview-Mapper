/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.IOException;
import java.nio.file.Path;

public abstract class CachedImageWriter implements ImageWriter {

	protected abstract Path getOutputDirectory();

	@Override
	public Path cacheBackupFile() throws IOException {
		return this.getOutputDirectory().resolve("render-cache.binlog");
	}

	@Override
	public Path cacheFile() throws IOException {
		return this.getOutputDirectory().resolve("render-cache.bak.binlog");
	}

	@Override
	public boolean supportsCache() {
		return true;
	}

}
