/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.engine;

public class RenderException extends Exception {

	private static final long serialVersionUID = -8020367716363859786L;

	public RenderException() {
	}

	public RenderException(String message) {
		super(message);
	}

	public RenderException(String message, Throwable cause) {
		super(message, cause);
	}

	public RenderException(Throwable cause) {
		super(cause);
	}

}
