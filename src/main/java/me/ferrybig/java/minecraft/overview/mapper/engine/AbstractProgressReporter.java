/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;

public abstract class AbstractProgressReporter implements ProgressReporter {

	@Override
	public void onFileEnd(WorldFile file) {
	}

	@Override
	public void onFileStart(WorldFile file) {
	}

	@Override
	public void onProgress(double progress, int processed, int total) {
	}

	@Override
	public void onRenderFinalize() {
	}

	@Override
	public void onRenderStart() {
	}

}
