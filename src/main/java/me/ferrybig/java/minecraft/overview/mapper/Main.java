/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap;
import me.ferrybig.java.minecraft.overview.mapper.render.BlockMap;
import me.ferrybig.java.minecraft.overview.mapper.render.DefaultImageRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleHTMLOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleRenderer;

public class Main {

    public static void main(String... list) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("test.html"))) {
            RegionRenderer rend = new DefaultImageRenderer(BlockMap.loadDefault(), BiomeMap.loadDefault());
            SimpleHTMLOutputRenderer render = new SimpleHTMLOutputRenderer(rend, writer, "gif");
            render.startRender();
            File root = new File("H:/world");
            List<File> files = new ArrayList<>();
            files.add(new File(root,"level.dat"));
            files.addAll(Arrays.asList(new File(root, "region").listFiles())); 
            final int size = files.size();
            
            for (int i = 0; i < size; i++) {
                File file = files.get(i);
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    System.out.println(i + "/" + size + ": "+file.getName() + ": start");
                    render.addFile(file.getName(), in);
                    System.out.println(i + "/" + size + ": "+file.getName() + ": done");
                }
            }
            render.finishRender();
        }
    }

    private static class Builder {

    }
}
