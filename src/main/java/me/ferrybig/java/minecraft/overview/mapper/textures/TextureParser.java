/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
	private final Map<String, UnresolvedBlockState> variants = new HashMap<>();
	private static final String BLOCKSTATES_PREFIX = "blockstates/";
	private static final String BLOCKSTATES_SUFFIX = ".json";

	private static InputStream readFromZipList(List<ZipFile> files, String filename) throws IOException {
		for(ZipFile file : files) {
			ZipEntry entry = file.getEntry(filename);
			if(entry != null) {
				return file.getInputStream(entry);
			}
		}
		throw new FileNotFoundException("File not found: " + filename);
	}

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
					materials.add(materialName);
				}
			}
			// Discovery done, its now time to resolve all files
			JsonParser parser = new JsonParser();
			Map<String, Model> modelCache = new HashMap<>();
			for(String material : materials) {
				try {
					JsonObject mainMaterial;
					try(Reader materialInputStream = new InputStreamReader(
						readFromZipList(zipFiles, BLOCKSTATES_PREFIX + material + BLOCKSTATES_SUFFIX),
						StandardCharsets.UTF_8
					)) {
						mainMaterial = parser.parse(materialInputStream).getAsJsonObject();
					}
				} catch(IOException e) {
					throw new IOException("Unable to parse material " + material, e);
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
		return null;
	}

	public UnresolvedBlockState getMaterial(String material) {
		return variants.get(material);
	}

	public Variant getMaterial(String material, SortedMap<String, String> map) {
		return this.getMaterial(material).resolve(map);
	}


}
