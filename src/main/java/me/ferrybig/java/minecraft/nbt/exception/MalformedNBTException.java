/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt.exception;

public class MalformedNBTException extends NBTException {

	public MalformedNBTException() {
	}

	public MalformedNBTException(String message) {
		super(message);
	}

	public MalformedNBTException(String message, Throwable cause) {
		super(message, cause);
	}

	public MalformedNBTException(Throwable cause) {
		super(cause);
	}

}
