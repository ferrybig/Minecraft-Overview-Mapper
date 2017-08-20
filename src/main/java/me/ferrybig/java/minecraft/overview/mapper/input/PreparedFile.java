/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.IOException;
import java.io.InputStream;

public abstract class PreparedFile {

	private final String name;

	public PreparedFile(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract InputStream openInputstream() throws IOException;

	public static PreparedFile of(String name, StreamProvider io) {
		return new PreparedFile(name) {
			@Override
			public InputStream openInputstream() throws IOException {
				return io.get();
			}
		};
	}

	public static interface StreamProvider {

		public InputStream get() throws IOException;
	}

}
