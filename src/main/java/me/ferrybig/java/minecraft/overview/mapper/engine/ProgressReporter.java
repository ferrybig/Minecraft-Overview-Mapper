/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;

public interface ProgressReporter {

	public void onProgress(double progress, int processed, int total);

	public void onFileStart(WorldFile file);

	public void onFileEnd(WorldFile file);

	public void onRenderFinalize();

	public void onRenderStart();
}
