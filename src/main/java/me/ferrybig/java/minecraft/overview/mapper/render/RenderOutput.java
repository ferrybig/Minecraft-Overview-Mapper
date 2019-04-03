/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.annotation.Nullable;

public class RenderOutput {

	@Nullable
	private final BufferedImage output;
	private final int lastModification;

	public RenderOutput(@Nullable BufferedImage output, int lastModification) {
		this.output = output;
		this.lastModification = lastModification;
	}

	@Nullable
	public BufferedImage getOutput() {
		return output;
	}

	public int getLastModification() {
		return lastModification;
	}

	@Override
	public String toString() {
		return "RenderOutput{" + "output=" + output + ", lastModification=" + lastModification + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + Objects.hashCode(this.output);
		hash = 17 * hash + this.lastModification;
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
		final RenderOutput other = (RenderOutput) obj;
		if (this.lastModification != other.lastModification) {
			return false;
		}
		if (!Objects.equals(this.output, other.output)) {
			return false;
		}
		return true;
	}

}
