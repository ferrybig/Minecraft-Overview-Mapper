/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TextureTest {
	public static void main(String ... args) throws IOException {
		TextureParser parser = new TextureParser();
		parser.readAll(Arrays.asList(new File("C:\\Users\\Fernando\\AppData\\Roaming\\.minecraft\\versions\\1.13.2\\1.13.2.jar")));
		System.out.println(parser.getMaterial("bedrock"));
	}
}
