/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import javax.annotation.Nonnull;

public class Rotation {
	private final @Nonnull Vector3d origin;
	private final @Nonnull Axis rotation;
	/**
	 * degrees, default 0
	 */
	private final double angle;
	private final boolean rescale;

	public Rotation(@Nonnull Vector3d origin, @Nonnull Axis rotation, double angle, boolean rescale) {
		this.origin = origin;
		this.rotation = rotation;
		this.angle = angle;
		this.rescale = rescale;
	}

	public Vector3d getOrigin() {
		return origin;
	}

	public Axis getRotation() {
		return rotation;
	}

	public double getAngle() {
		return angle;
	}

	public boolean isRescale() {
		return rescale;
	}

	public boolean isNoop() {
		return angle == 0;
	}
}
