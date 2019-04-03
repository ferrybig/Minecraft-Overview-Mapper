/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Objects;
import javax.annotation.Nonnull;

public class Rotation {

	private final @Nonnull
	Vector3d origin;
	private final @Nonnull
	Axis rotation;
	/**
	 * degrees, default 0
	 */
	private final double angle;
	private final boolean rescale;
	public static final Rotation NOOP = new Rotation(new Vector3d(0, 0, 0), Axis.X, 0, false);

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

	@Override
	public String toString() {
		return "Rotation{" + "origin=" + origin + ", rotation=" + rotation + ", angle=" + angle + ", rescale=" + rescale + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + Objects.hashCode(this.origin);
		hash = 83 * hash + Objects.hashCode(this.rotation);
		hash = 83 * hash + (int) (Double.doubleToLongBits(this.angle) ^ (Double.doubleToLongBits(this.angle) >>> 32));
		hash = 83 * hash + (this.rescale ? 1 : 0);
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
		final Rotation other = (Rotation) obj;
		if (Double.doubleToLongBits(this.angle) != Double.doubleToLongBits(other.angle)) {
			return false;
		}
		if (this.rescale != other.rescale) {
			return false;
		}
		if (!Objects.equals(this.origin, other.origin)) {
			return false;
		}
		if (this.rotation != other.rotation) {
			return false;
		}
		return true;
	}
}
