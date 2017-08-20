package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.PreparedFile;

public interface RenderEngine {

	public void startRender() throws IOException;

	public void addFile(String fileName, InputStream in) throws IOException;

	public void finishRender() throws IOException;

	public boolean isConcurrent();

	public RenderEngine fork();

	public void merge(RenderEngine fork);

	public default void forInputSource(InputSource input, Consumer<String> beforeFile, Consumer<String> afterFile) throws IOException {
		try (Stream<PreparedFile> stream = input.stream()) {
			startRender();
			Iterator<PreparedFile> iterator = stream.iterator();
			while (iterator.hasNext()) {
				PreparedFile file = iterator.next();
				beforeFile.accept(file.getName());
				InputStream openInputstream = file.openInputstream();
				addFile(file.getName(), openInputstream);
				input.closeStreamIfNeeded(openInputstream);
				afterFile.accept(file.getName());
			}
			finishRender();
		}
	}
}
