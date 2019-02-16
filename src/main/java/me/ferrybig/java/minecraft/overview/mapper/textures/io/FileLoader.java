/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileLoader implements Closeable {

	private final List<ZipFile> files;

	public FileLoader() {
		this.files = new ArrayList<>();
	}

	public FileLoader(int expectedSize) {
		this.files = new ArrayList<>(expectedSize);
	}

	public void addFile(File file) throws IOException {
		// todo support loading resource pack from directory
		this.files.add(new ZipFile(file));
	}

	@Override
	public void close() throws IOException {
		List<IOException> exList = new ArrayList<>();
		for (ZipFile file : this.files) {
			try {
				file.close();
			} catch (IOException e) {
				exList.add(e);
			}
		}
		switch (exList.size()) {
			case 0:
				return;
			case 1:
				throw exList.get(0);
			default:
				IOException e = new IOException("Unexpected error during closing of resources");
				for (IOException ex : exList) {
					e.addSuppressed(ex);
				}
				throw e;
		}
	}

	public InputStream loadPath(String path) throws IOException, FileNotFoundException {
		InputStream in = tryLoadPath(path);
		if (in == null) {
			throw new FileNotFoundException(path);
		}
		return in;
	}

	public InputStream tryLoadPath(String path) throws IOException {
		for (ZipFile file : this.files) {
			ZipEntry entry = file.getEntry(path);
			if (entry != null) {
				return file.getInputStream(entry);
			}
		}
		return null;
	}

	public List<String> allFiles() {
		SortedSet<String> names = new TreeSet<>();
		for (ZipFile file : this.files) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				names.add(entries.nextElement().getName());
			}
		}
		return new ArrayList<>(names);
	}
}
