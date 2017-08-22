/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public interface RegionRenderer {

	public BufferedImage renderFile(String fileName, InputStream in)
			throws IOException;
}
