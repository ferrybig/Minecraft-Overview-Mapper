/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.textures;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	private static final String BASE_PREFIX = "assets/minecraft/";
	private static final String BLOCKSTATES_PREFIX = BASE_PREFIX + "blockstates/";
	private static final String BLOCKSTATES_SUFFIX = ".json";
	private static final String BLOCKS_PREFIX = BASE_PREFIX + "models/";
	private static final String BLOCKS_SUFFIX = ".json";

	private static InputStream readFromZipList(List<ZipFile> files, String filename) throws IOException {
		for(ZipFile file : files) {
			ZipEntry entry = file.getEntry(filename);
			if(entry != null) {
				return file.getInputStream(entry);
			}
		}
		throw new FileNotFoundException("File not found: " + filename);
	}
	private static Reader readTextFromZipList(List<ZipFile> files, String filename) throws IOException {
		return new BufferedReader(new InputStreamReader(readFromZipList(files, filename), StandardCharsets.UTF_8));
	}

	private static Model readModel(List<ZipFile> files, Map<String, Model> modelCache, JsonParser parser, String modelName) throws IOException {
		if(modelCache.containsKey(modelName)) {
			Model model = modelCache.get(modelName);
			if(model == null) {
				throw new IllegalStateException("Model " + modelName + " got requested while its still fetching");
			}
			return model;
		}
		modelCache.put(modelName, null);
		JsonObject model;
		try(Reader in = readTextFromZipList(files, BLOCKS_PREFIX + modelName + BLOCKS_SUFFIX)) {
			model = parser.parse(in).getAsJsonObject();
		}
		final Model parent;
		final List<Cube> cubes;
		final Map<String, String> textures = new HashMap<>();
		{
			JsonElement optionalParent = model.get("parent");
			if(optionalParent != null) {
				parent = readModel(files, modelCache, parser, optionalParent.getAsString());
			} else {
				parent = null;
			}
		}
		{
			JsonArray optionalCubes = model.getAsJsonArray("elements");
			if(optionalCubes != null) {
				cubes = new ArrayList<>(optionalCubes.size());
				Iterator<JsonElement> iterator = optionalCubes.iterator();
				while(iterator.hasNext()) {
					JsonObject cube = iterator.next().getAsJsonObject();
					final Vector3d from;
					final Vector3d to;
					final Face down;
					final Face up;
					final Face north;
					final Face south;
					final Face east;
					final Face west;
					final Rotation rotation;
					boolean shade;
					{
						JsonArray fromArray = cube.getAsJsonArray("from");
						from = new Vector3d(fromArray.get(0).getAsDouble(), fromArray.get(1).getAsDouble(), fromArray.get(2).getAsDouble());
					}
					{
						JsonArray toArray = cube.getAsJsonArray("to");
						to = new Vector3d(toArray.get(0).getAsDouble(), toArray.get(1).getAsDouble(), toArray.get(2).getAsDouble());
					}
					{
						JsonObject faces = cube.getAsJsonObject("faces");
						down = null;
						up = null;
						north = null;
						south = null;
						west = null;
						east = null;
					}
					{
						rotation = Rotation.NOOP;
					}
					{
						shade = false;
					}
					cubes.add(new Cube(from, to, down, up, north, south, west, east, rotation, shade));
				}
			} else {
				if(parent == null) {
					cubes = Collections.emptyList();
				} else {
					cubes = parent.getElements();
				}
			}
		}
		return new Model(cubes, textures, parent);
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
					try(Reader materialInputStream = readTextFromZipList(zipFiles, BLOCKSTATES_PREFIX + material + BLOCKSTATES_SUFFIX)) {
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
