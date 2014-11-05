package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.IOException;
import java.io.InputStream;

public interface RenderEngine {
    public void startRender() throws IOException;
    public void addFile(String fileName, InputStream in) throws IOException;
    public void finishRender() throws IOException;
}
