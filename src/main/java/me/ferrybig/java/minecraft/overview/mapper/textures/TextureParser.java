/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.DefaultVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.blockstate.UnresolvedBlockState;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;

public class TextureParser {
	private final Map<String, Variant> variants = new HashMap<>();
	private static final String BLOCKSTATES_PREFIX = "blockstates/";
	private static final String BLOCKSTATES_SUFFIX = ".json";

	public void readAll(List<File> files) throws IOException {
		List<ZipFile> zipFiles = new ArrayList<>(files.size());
		try {
			for(File file : files) {
				zipFiles.add(new ZipFile(file));
			}
			Set<String> materials = new HashSet<>();
			for(ZipFile file : zipFiles) {
				Enumeration<? extends ZipEntry> list = file.entries();
				while(list.hasMoreElements()) {
					ZipEntry entry = list.nextElement();
					if(entry.isDirectory()) {
						continue;
					}
					String name = entry.getName();
					if(!name.startsWith(BLOCKSTATES_PREFIX)) {
						continue;
					}
					String materialName = name.substring(BLOCKSTATES_PREFIX.length(), name.length() - BLOCKSTATES_SUFFIX.length());

				}
			}
		} finally {
			for(ZipFile file : zipFiles) {
				try {
					file.close();
				} catch(Exception e) {
				}
			}
		}

	}

	private DefaultVariant parseTexture(JsonObject object, DefaultVariant parent) {
		
	}

	public UnresolvedBlockState getMaterial(String material) {

	}

	public Variant getMaterial(String material, SortedMap<String, String> map) {
		return this.getMaterial(material).resolve(map);
	}


}
