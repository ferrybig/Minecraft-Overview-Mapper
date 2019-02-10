/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Objects;
import javax.annotation.Nullable;

public class Cube {

	private final Vector3d from;
	private final Vector3d to;
	private final @Nullable
	Face down;
	private final @Nullable
	Face up;
	private final @Nullable
	Face north;
	private final @Nullable
	Face south;
	private final @Nullable
	Face west;
	private final @Nullable
	Face east;
	private final Rotation rotation;
	private final boolean shade;

	public Cube(Vector3d from, Vector3d to, Face down, Face up, Face north, Face south, Face west, Face east, Rotation rotation) {
		this(from, to, down, up, north, south, west, east, rotation, true);
	}

	public Cube(Vector3d from, Vector3d to, Face down, Face up, Face north, Face south, Face west, Face east, Rotation rotation, boolean shade) {
		this.from = from;
		this.to = to;
		this.down = down;
		this.up = up;
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.rotation = rotation;
		this.shade = shade;
	}

	public Vector3d getFrom() {
		return from;
	}

	public Vector3d getTo() {
		return to;
	}

	public Face getDown() {
		return down;
	}

	public Face getUp() {
		return up;
	}

	public Face getNorth() {
		return north;
	}

	public Face getSouth() {
		return south;
	}

	public Face getWest() {
		return west;
	}

	public Face getEast() {
		return east;
	}

	public Rotation getRotation() {
		return rotation;
	}

	public boolean isShade() {
		return shade;
	}

	@Override
	public String toString() {
		return "Cube{" + "from=" + from + ", to=" + to + ", down=" + down + ", up=" + up + ", north=" + north + ", south=" + south + ", west=" + west + ", east=" + east + ", rotation=" + rotation + ", shade=" + shade + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41 * hash + Objects.hashCode(this.from);
		hash = 41 * hash + Objects.hashCode(this.to);
		hash = 41 * hash + Objects.hashCode(this.down);
		hash = 41 * hash + Objects.hashCode(this.up);
		hash = 41 * hash + Objects.hashCode(this.north);
		hash = 41 * hash + Objects.hashCode(this.south);
		hash = 41 * hash + Objects.hashCode(this.west);
		hash = 41 * hash + Objects.hashCode(this.east);
		hash = 41 * hash + Objects.hashCode(this.rotation);
		hash = 41 * hash + (this.shade ? 1 : 0);
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
		final Cube other = (Cube) obj;
		if (this.shade != other.shade) {
			return false;
		}
		if (!Objects.equals(this.from, other.from)) {
			return false;
		}
		if (!Objects.equals(this.to, other.to)) {
			return false;
		}
		if (!Objects.equals(this.down, other.down)) {
			return false;
		}
		if (!Objects.equals(this.up, other.up)) {
			return false;
		}
		if (!Objects.equals(this.north, other.north)) {
			return false;
		}
		if (!Objects.equals(this.south, other.south)) {
			return false;
		}
		if (!Objects.equals(this.west, other.west)) {
			return false;
		}
		if (!Objects.equals(this.east, other.east)) {
			return false;
		}
		if (!Objects.equals(this.rotation, other.rotation)) {
			return false;
		}
		return true;
	}

}
