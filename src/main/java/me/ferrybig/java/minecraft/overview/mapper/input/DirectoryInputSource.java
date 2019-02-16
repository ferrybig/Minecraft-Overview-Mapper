/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DirectoryInputSource implements InputSource {

	private final File root;

	public DirectoryInputSource(File root) {
		this.root = root;
	}

	@Override
	public Stream<PreparedFile> stream() {
		List<File> files = new ArrayList<>();
		files.add(new File(root, "level.dat"));
		files.addAll(Arrays.asList(new File(root, "region").listFiles()));
		return files.stream().map(f
			-> PreparedFile.of(f.getName(), () -> new FileInputStream(f))
		);
	}

}
