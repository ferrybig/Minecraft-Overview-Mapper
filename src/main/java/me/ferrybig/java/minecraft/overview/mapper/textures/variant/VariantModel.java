/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.variant;

import java.util.Objects;
import me.ferrybig.java.minecraft.overview.mapper.textures.Model;

public class VariantModel {

	private final Model model;
	private final double xRotation;
	private final double yRotation;
	private final boolean uvLock;

	public VariantModel(Model model, double xRotation, double yRotation, boolean uvLock) {
		this.model = model;
		this.xRotation = xRotation;
		this.yRotation = yRotation;
		this.uvLock = uvLock;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 29 * hash + Objects.hashCode(this.model);
		hash = 29 * hash + (int) (Double.doubleToLongBits(this.xRotation) ^ (Double.doubleToLongBits(this.xRotation) >>> 32));
		hash = 29 * hash + (int) (Double.doubleToLongBits(this.yRotation) ^ (Double.doubleToLongBits(this.yRotation) >>> 32));
		hash = 29 * hash + (this.uvLock ? 1 : 0);
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
		final VariantModel other = (VariantModel) obj;
		if (Double.doubleToLongBits(this.xRotation) != Double.doubleToLongBits(other.xRotation)) {
			return false;
		}
		if (Double.doubleToLongBits(this.yRotation) != Double.doubleToLongBits(other.yRotation)) {
			return false;
		}
		if (this.uvLock != other.uvLock) {
			return false;
		}
		if (!Objects.equals(this.model, other.model)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "VariantModel{" + "model=" + model + ", xRotation=" + xRotation + ", yRotation=" + yRotation + ", uvLock=" + uvLock + '}';
	}

	public Model getModel() {
		return model;
	}

	public double getxRotation() {
		return xRotation;
	}

	public double getyRotation() {
		return yRotation;
	}

	public boolean isUvLock() {
		return uvLock;
	}

}
