/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt;

public abstract class SimpleTag extends Tag {

	public SimpleTag(TagType type) {
		super(type);
	}

	@Override
	protected Resolveable resolveNode(String path) {
		throw new UnsupportedOperationException("A " + this.type + " tag does not have a child called " + path);
	}

}
