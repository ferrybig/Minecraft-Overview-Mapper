/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper;

import java.util.List;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.engine.RenderEngine;
import me.ferrybig.java.minecraft.overview.mapper.engine.RenderOptions;

public class ProgramOptions {

	@Nonnull
	private final List<RenderOptions> jobs;
	@Nonnull
	private final RenderEngine engine;

	public ProgramOptions(List<RenderOptions> jobs, RenderEngine engine) {
		this.jobs = jobs;
		this.engine = engine;
	}

	public List<RenderOptions> getJobs() {
		return jobs;
	}

	public RenderEngine getEngine() {
		return engine;
	}

}
