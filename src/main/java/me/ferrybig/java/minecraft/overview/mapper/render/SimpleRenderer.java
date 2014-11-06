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

    @Override
    public void addFile(String fileName, InputStream in) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    protected abstract void addLevelDat(CompoundTag level) throws IOException;
    
    protected abstract void addImage(BufferedImage tile, int x, int z) throws IOException;

}
