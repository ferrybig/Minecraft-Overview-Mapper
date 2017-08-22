/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public interface InputSource {

	public Stream<PreparedFile> stream() throws IOException;

	public default void closeStreamIfNeeded(InputStream in) throws IOException {
		in.close();
	}
}
