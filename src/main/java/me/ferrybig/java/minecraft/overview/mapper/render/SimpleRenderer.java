/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.jnbt.CompoundTag;

public abstract class SimpleRenderer implements RenderEngine {

    private final RegionRenderer renderer;

    public SimpleRenderer(RegionRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void addFile(String fileName, InputStream in) throws IOException {
        renderer.renderFile(fileName, in);
    }
    
    protected abstract void addLevelDat(CompoundTag level) throws IOException;
    
    protected abstract void addImage(BufferedImage tile, int x, int z) throws IOException;

}
