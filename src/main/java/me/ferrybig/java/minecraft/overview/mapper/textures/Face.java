/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.util.Objects;

public class Face {

	private final Vector2d point1;
	private final Vector2d point2;
	private final String texture;
	private final CullFace cullFace;
	private final int rotation;
	private final int tintIndex;
	private final boolean usesTintIndex;

	public Face(Vector2d point1, Vector2d point2, String texture, CullFace cullFace, int rotation, int tintIndex, boolean usesTintIndex) {
		this.point1 = point1;
		this.point2 = point2;
		this.texture = texture;
		this.cullFace = cullFace;
		this.rotation = rotation;
		this.tintIndex = tintIndex;
		this.usesTintIndex = usesTintIndex;
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

	public CullFace getCullFace() {
		return cullFace;
	}

	public int getRotation() {
		return rotation;
	}

	public int getTintIndex() {
		return tintIndex;
	}

	public boolean usesTintIndex() {
		return usesTintIndex;
	}

	@Override
	public String toString() {
		return "Face{" + "point1=" + point1 + ", point2=" + point2 + ", texture=" + texture + ", cullFace=" + cullFace + ", rotation=" + rotation + ", tintIndex=" + tintIndex + ", usesTintIndex=" + usesTintIndex + '}';
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 73 * hash + Objects.hashCode(this.point1);
		hash = 73 * hash + Objects.hashCode(this.point2);
		hash = 73 * hash + Objects.hashCode(this.texture);
		hash = 73 * hash + Objects.hashCode(this.cullFace);
		hash = 73 * hash + this.rotation;
		hash = 73 * hash + this.tintIndex;
		hash = 73 * hash + (this.usesTintIndex ? 1 : 0);
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
		final Face other = (Face) obj;
		if (this.rotation != other.rotation) {
			return false;
		}
		if (this.tintIndex != other.tintIndex) {
			return false;
		}
		if (this.usesTintIndex != other.usesTintIndex) {
			return false;
		}
		if (!Objects.equals(this.texture, other.texture)) {
			return false;
		}
		if (!Objects.equals(this.point1, other.point1)) {
			return false;
		}
		if (!Objects.equals(this.point2, other.point2)) {
			return false;
		}
		if (this.cullFace != other.cullFace) {
			return false;
		}
		return true;
	}

}
