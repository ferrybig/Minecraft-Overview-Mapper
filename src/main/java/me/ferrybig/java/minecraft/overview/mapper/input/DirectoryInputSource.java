/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class DirectoryInputSource implements InputSource {

	private final Path root;

	public DirectoryInputSource(Path root) {
		this.root = root;
	}

	@Override
	public InputInfo generateFileListing() throws IOException {
		ArrayList<WorldFile> files = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String path = root.relativize(file).toString();
				if (!File.separator.equals("/")) {
					path = path.replace(File.separator, "/");
				}
				files.add(WorldFile.of(path));
				return FileVisitResult.CONTINUE;
			}
		});
		files.forEach(System.out::println);
		return new SimpleInputInfo(files) {
			@Override
			protected PreparedFile toPreparedFile(WorldFile file) throws IOException {
				return PreparedFile.of(file, root.resolve(file.getOrignalName()));
			}
		};
	}

}
