/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import javax.annotation.Nullable;

public class Cube {
	private final Vector3d from;
	private final Vector3d to;
	private final @Nullable Face down;
	private final @Nullable Face up;
	private final @Nullable Face north;
	private final @Nullable Face south;
	private final @Nullable Face west;
	private final @Nullable Face east;
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


}
