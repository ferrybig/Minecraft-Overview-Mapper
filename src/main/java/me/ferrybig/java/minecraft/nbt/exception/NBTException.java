/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt.exception;

public class NBTException extends Exception {

	private static final long serialVersionUID = -5505405570531780532L;

	public NBTException() {
	}

	public NBTException(String message) {
		super(message);
	}

	public NBTException(String message, Throwable cause) {
		super(message, cause);
	}

	public NBTException(Throwable cause) {
		super(cause);
	}

}
