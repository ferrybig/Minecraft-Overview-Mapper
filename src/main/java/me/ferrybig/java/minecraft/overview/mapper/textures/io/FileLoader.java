/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures.io;

import com.google.gson.JsonElement;
import java.awt.image.BufferedImage;
import java.io.IOException;

public interface FileLoader {

	public BufferedImage loadTexture(String path) throws IOException;

	public JsonElement loadFile(String path) throws IOException;
}
