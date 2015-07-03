/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;

public abstract class SimpleRenderer implements RenderEngine {

    private static final Pattern rfpat
        = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");
    private final RegionRenderer renderer;

    public SimpleRenderer(RegionRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void addFile(String fileName, InputStream in) throws IOException {
        Matcher localMatcher;
        if ((localMatcher = rfpat.matcher(fileName)).matches()) {
            addImage(renderer.renderFile(fileName, in),
                Integer.parseInt((localMatcher.group(1))),
                Integer.parseInt((localMatcher.group(2))));
        } else if (fileName.equals("level.dat")) {
            this.addLevelDat((CompoundTag) new NBTInputStream(in).readTag());
        } else if (fileName.endsWith("mcr")) {
            
        } else {
            throw new IOException("Unknown file: "+fileName);
        }
    }

    protected abstract void addLevelDat(CompoundTag level) throws IOException;

    protected abstract void addImage(BufferedImage tile, int x, int z)
        throws IOException;

}
