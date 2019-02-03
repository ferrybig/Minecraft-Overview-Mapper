/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

public class Face {
	private final Vector2d point1;
	private final Vector2d point2;
	private final String texture;

	public Face(Vector2d point1, Vector2d point2, String texture) {
		this.point1 = point1;
		this.point2 = point2;
		this.texture = texture;
	}

	public Vector2d getPoint1() {
		return point1;
	}

	public Vector2d getPoint2() {
		return point2;
	}

	public String getTexture() {
		return texture;
	}

	@Override
	public String toString() {
		return "Face{" + "point1=" + point1 + ", point2=" + point2 + ", texture=" + texture + '}';
	}
}
