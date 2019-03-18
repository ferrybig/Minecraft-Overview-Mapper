/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.nbt.exception;

public class UnknownNBTException extends NBTException {

	public UnknownNBTException() {
	}

	public UnknownNBTException(String message) {
		super(message);
	}

	public UnknownNBTException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownNBTException(Throwable cause) {
		super(cause);
	}

}
