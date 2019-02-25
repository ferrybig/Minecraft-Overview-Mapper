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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import me.ferrybig.java.minecraft.overview.mapper.textures.blockstate.MultiBlockState;
import me.ferrybig.java.minecraft.overview.mapper.textures.blockstate.UnresolvedBlockState;
import me.ferrybig.java.minecraft.overview.mapper.textures.io.FileLoader;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.SimpleVariant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.AndVariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.AnyVariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.OrVariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.SimpleVariantSpecifier;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.specifier.VariantSpecifier;

public class TextureParser {

	private final Map<String, UnresolvedBlockState> variants = new HashMap<>();
	private final Map<String, BufferedImage> textures = new HashMap<>();
	private static final String BASE_PREFIX = "assets/minecraft/";
	private static final String BLOCKSTATES_PREFIX = BASE_PREFIX + "blockstates/";
	private static final String BLOCKSTATES_SUFFIX = ".json";
	private static final String BLOCKS_PREFIX = BASE_PREFIX + "models/";
	private static final String BLOCKS_SUFFIX = ".json";
	private static final String TEXTURES_PREFIX = BASE_PREFIX + "textures/";
	private static final String TEXTURES_SUFFIX = ".png";
	private static final BufferedImage IMAGE_NOT_FOUND;
	private static final BufferedImage IMAGE_ERROR;

	static {
		IMAGE_NOT_FOUND = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = IMAGE_NOT_FOUND.createGraphics();
		try {
			g2.setColor(Color.red);
			g2.fillRect(0, 0, 8, 8);
			g2.fillRect(8, 8, 8, 8);
		} finally {
			g2.dispose();
		}
	}

	static {
		IMAGE_ERROR = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = IMAGE_ERROR.createGraphics();
		try {
			g2.setColor(Color.yellow);
			g2.fillRect(0, 0, 8, 8);
			g2.fillRect(8, 8, 8, 8);
		} finally {
			g2.dispose();
		}
	}

	private static Reader readTextFromZipList(FileLoader loader, String filename) throws IOException {
		return new BufferedReader(new InputStreamReader(loader.loadPath(filename), StandardCharsets.UTF_8));
	}

	private static Face readFace(JsonElement face, Vector2d defaultPoint1, Vector2d defaultPoint2) {
		if (face == null) {
			return null;
		}
		JsonObject obj = face.getAsJsonObject();
		// todo uv, cullFace, rotation, tintindex
		final Vector2d point1;
		final Vector2d point2;
		final String texture = obj.get("texture").getAsString();
		final CullFace cullFace = obj.has("cullface")
			? CullFace.valueOf(obj.get("cullface").getAsString().toUpperCase())
			: null;
		final int rotation = obj.has("rotation")
			? obj.get("rotation").getAsInt()
			: 0;
		final int tintIndex;
		final boolean usesTintIndex;
		{
			JsonArray uv = obj.getAsJsonArray("uv");
			if (uv != null) {
				point1 = new Vector2d(uv.get(0).getAsInt(), uv.get(1).getAsInt());
				point2 = new Vector2d(uv.get(2).getAsInt(), uv.get(3).getAsInt());
			} else {
				point1 = defaultPoint1;
				point2 = defaultPoint2;
			}
		}
		{
			JsonElement tintIndexElement = obj.get("tintindex");
			if (tintIndexElement != null) {
				usesTintIndex = true;
				tintIndex = tintIndexElement.getAsInt();
			} else {
				usesTintIndex = false;
				tintIndex = 0;
			}
		}
		return new Face(point1, point2, texture, cullFace, rotation, tintIndex, usesTintIndex);
	}

	private static Model readModel(FileLoader files, Map<String, Model> modelCache, JsonParser parser, String modelName) throws IOException {
		if (modelCache.containsKey(modelName)) {
			Model model = modelCache.get(modelName);
			if (model == null) {
				throw new IllegalStateException("Model " + modelName + " got requested while its still fetching");
			}
			return model;
		}
		System.err.println("Loading model block/" + modelName + "...");
		modelCache.put(modelName, null);
		JsonObject model;
		try (Reader in = readTextFromZipList(files, BLOCKS_PREFIX + modelName + BLOCKS_SUFFIX)) {
			model = parser.parse(in).getAsJsonObject();
		}
		final Model parent;
		final List<Cube> cubes;
		final Map<String, String> textures = new HashMap<>();
		{
			JsonElement optionalParent = model.get("parent");
			if (optionalParent != null) {
				parent = readModel(files, modelCache, parser, optionalParent.getAsString());
				textures.putAll(parent.getTexture());
			} else {
				parent = null;
			}
		}
		{
			JsonArray optionalCubes = model.getAsJsonArray("elements");
			if (optionalCubes != null) {
				cubes = new ArrayList<>(optionalCubes.size());
				Iterator<JsonElement> iterator = optionalCubes.iterator();
				while (iterator.hasNext()) {
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
						// TODO faces { "texture": "#down", "cullface": "down" }
						down = readFace(
							faces.get("down"),
							new Vector2d(from.getX(), from.getZ()),
							new Vector2d(to.getX(), to.getZ())
						);
						up = readFace(
							faces.get("up"),
							new Vector2d(from.getX(), from.getZ()),
							new Vector2d(to.getX(), to.getZ())
						);
						north = readFace(faces.get("north"),
							new Vector2d(from.getY(), from.getZ()),
							new Vector2d(to.getY(), to.getZ())
						);
						south = readFace(faces.get("south"),
							new Vector2d(from.getY(), from.getZ()),
							new Vector2d(to.getY(), to.getZ())
						);
						west = readFace(faces.get("west"),
							new Vector2d(from.getX(), from.getY()),
							new Vector2d(to.getX(), to.getY())
						);
						east = readFace(faces.get("east"),
							new Vector2d(from.getX(), from.getY()),
							new Vector2d(to.getX(), to.getY())
						);
					}
					{
						rotation = Rotation.NOOP;
					}
					{
						shade = cube.has("shade") ? cube.get("shade").getAsBoolean() : false;
					}
					cubes.add(new Cube(from, to, down, up, north, south, west, east, rotation, shade));
				}
			} else {
				if (parent == null) {
					cubes = Collections.emptyList();
				} else {
					cubes = parent.getElements();
				}
			}
		}
		{
			JsonObject optionalTextures = model.getAsJsonObject("textures");
			if (optionalTextures != null) {
				Iterator<Map.Entry<String, JsonElement>> iterator = optionalTextures.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, JsonElement> next = iterator.next();
					textures.put(next.getKey(), next.getValue().getAsString());
				}
			}
		}
		Model modelObj = new Model(cubes, textures, parent);
		modelCache.put(modelName, modelObj);
		return modelObj;
	}

	private static Map.Entry<VariantSpecifier, VariantModel> readModelParts(
		Map<String, Model> modelCache, FileLoader loader,
		VariantSpecifier specifier, JsonElement modelData,
		JsonParser parser
	) throws IOException {
		if (modelData.isJsonArray()) {
			// todo support multiple weights
			return readModelPart(modelCache, loader, specifier, modelData.getAsJsonArray().get(0).getAsJsonObject(), parser);
		}
		return readModelPart(modelCache, loader, specifier, modelData.getAsJsonObject(), parser);
	}

	private static Map.Entry<VariantSpecifier, VariantModel> readModelPart(
		Map<String, Model> modelCache, FileLoader loader,
		VariantSpecifier specifier, JsonObject modelData,
		JsonParser parser
	) throws IOException {
		String modelName = modelData.get("model").getAsString();
		double rotationX = modelData.has("x") ? modelData.get("x").getAsDouble() : 0;
		double rotationY = modelData.has("y") ? modelData.get("y").getAsDouble() : 0;
		boolean uvLock = modelData.has("uvlock") ? modelData.get("uvlock").getAsBoolean() : false;
		VariantModel model = new VariantModel(readModel(loader, modelCache, parser, modelName), rotationX, rotationY, uvLock);
		return new AbstractMap.SimpleImmutableEntry<>(specifier, model);
	}

	private static VariantSpecifier readObjectToVariantSpecifier(JsonObject obj) {
		Set<Map.Entry<String, JsonElement>> entrySet = obj.entrySet();
		List<VariantSpecifier> specifiers = new ArrayList<>(entrySet.size());
		Iterator<Map.Entry<String, JsonElement>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonElement> next = iterator.next();
			String key = next.getKey();
			String[] values = next.getValue().getAsString().split("\\|");
			final VariantSpecifier specifier;
			if (values.length == 0) {
				specifier = VariantSpecifier.TRUE;
			} else {
				specifier = new AnyVariantSpecifier(key, values);
			}
			specifiers.add(specifier);
		}
		assert specifiers.size() == entrySet.size();
		if (specifiers.isEmpty()) {
			return VariantSpecifier.TRUE;
		} else if (specifiers.size() == 1) {
			return specifiers.get(0);
		} else {
			return new AndVariantSpecifier(specifiers.toArray(new VariantSpecifier[0]));
		}
	}

	public void readAll(List<File> files) throws IOException {
		try (FileLoader loader = new FileLoader(files.size())) {
			for (File file : files) {
				loader.addFile(file);
			}
			List<String> materials = new ArrayList<>();
			for (String name : loader.allFiles()) {
				if (!name.startsWith(BLOCKSTATES_PREFIX)) {
					continue;
				}
				String materialName = name.substring(BLOCKSTATES_PREFIX.length(), name.length() - BLOCKSTATES_SUFFIX.length());
				materials.add(materialName);
			}
			System.err.println("Discovered " + materials.size() + " blockstate");
			// Discovery done, its now time to resolve all files
			Map<String, UnresolvedBlockState> blockstates = new HashMap<>();
			Map<String, Model> modelCache = new HashMap<>();
			// Default blocks internally handles by mc
			{
				modelCache.put("block/water", new Model(
					Collections.singletonList(
						new Cube(
							new Vector3d(0, 0, 0), new Vector3d(16, 14, 16),
							null, new Face(
								new Vector2d(0, 0), new Vector2d(16, 16),
								"#water",
								null,
								0,
								0,
								true
							),
							null, null, null, null, Rotation.NOOP
						)
					), Collections.singletonMap("water", "block/water_still"), null
				));
			}
			{
				JsonParser parser = new JsonParser();
				for (String material : materials) {
					try {
						System.err.println("-------------------------------------");
						System.err.println("Loading blockstate: " + material + "...");
						final UnresolvedBlockState blockState;
						{
							JsonObject mainMaterial;
							ArrayList<Map.Entry<VariantSpecifier, VariantModel>> list = new ArrayList<>();
							try (Reader materialInputStream = readTextFromZipList(loader, BLOCKSTATES_PREFIX + material + BLOCKSTATES_SUFFIX)) {
								mainMaterial = parser.parse(materialInputStream).getAsJsonObject();
							}
							JsonElement variants = mainMaterial.get("variants");
							JsonElement multiPart = mainMaterial.get("multipart");
							if (variants != null && variants.isJsonObject()) {
								JsonObject variantsMap = variants.getAsJsonObject();
								list.ensureCapacity(list.size() + variantsMap.size());
								Iterator<Map.Entry<String, JsonElement>> variantsIteratorvariants = variantsMap.entrySet().iterator();
								while (variantsIteratorvariants.hasNext()) {
									Map.Entry<String, JsonElement> next = variantsIteratorvariants.next();
									// TODO support multi block variants
									final JsonElement nextObj = next.getValue();
									final VariantSpecifier key;
									{
										if (next.getKey().isEmpty()) {
											key = VariantSpecifier.TRUE;
										} else {
											String[] split = next.getKey().split(",");
											Map<String, String> states = new LinkedHashMap<>();
											for (String keyPair : split) {
												String[] keyPairSplit = keyPair.split("=");
												if (keyPairSplit.length != 2) {
													throw new IOException("Invalid keypair in '" + next.getKey() + "', = missing");
												}
												states.put(keyPairSplit[0], keyPairSplit[1]);
											}
											key = new SimpleVariantSpecifier(states);
										}
									}
									list.add(readModelParts(modelCache, loader, key, nextObj, parser));
								}
							} else if (multiPart != null && multiPart.isJsonArray()) {
								JsonArray array = multiPart.getAsJsonArray();
								int size = array.size();
								list.ensureCapacity(list.size() + size);
								for (int i = 0; i < size; i++) {
									JsonObject multipartObject = array.get(i).getAsJsonObject();
									JsonObject when = multipartObject.getAsJsonObject("when");
									VariantSpecifier specifier;
									if (when != null) {
										JsonArray or = when.getAsJsonArray("OR");
										if (or != null) {
											int orSize = or.size();
											List<VariantSpecifier> specifiers = new ArrayList<>(orSize);
											for (int j = 0; j < orSize; j++) {
												specifiers.add(readObjectToVariantSpecifier(or.get(j).getAsJsonObject()));
											}
											specifier = new OrVariantSpecifier(specifiers.toArray(new VariantSpecifier[0]));
										} else {
											specifier = readObjectToVariantSpecifier(when);
										}
									} else {
										specifier = VariantSpecifier.TRUE;
									}
									list.add(readModelParts(modelCache, loader, specifier, multipartObject.get("apply"), parser));
								}
							} else {
								throw new IOException("Unable to parse blockstate " + material + ": unknown format");
							}
							blockState = new MultiBlockState(list);
						}
						blockstates.put(material, blockState);
					} catch (IOException e) {
						throw new IOException("Unable to parse material " + material, e);
					}
				}
			}
			Map<String, BufferedImage> textures = new HashMap<>();
			{
				for (Model model : modelCache.values()) {
					for (String texture : model.getTexture().values()) {
						if (!texture.startsWith("#")) {
							// This is a texture we can load!
							try (InputStream in = loader.tryLoadPath(TEXTURES_PREFIX + texture + TEXTURES_SUFFIX)) {
								if (in == null) {
									textures.put(texture, IMAGE_NOT_FOUND);
								} else {
									textures.put(texture, ImageIO.read(in));
								}
							}
						}
					}
				}
			}
			this.textures.putAll(textures);
			this.variants.putAll(blockstates);
		}
	}

	public UnresolvedBlockState getMaterial(String material) {
		return variants.get(material);
	}

	public Variant getMaterial(String material, Map<String, String> map) {
		if(material.startsWith("minecraft:")) {
			material = material.substring("minecraft:".length());
		}
		final UnresolvedBlockState unresolvedMaterial = this.getMaterial(material);
		if(unresolvedMaterial == null) {
			throw new IllegalArgumentException("Material not found: " + material);
		}
		return unresolvedMaterial.resolve(map);
	}

	public BufferedImage getTexture(Model model, String textureName) {
		String textureKey = textureName;
		int loopCount = 0;
		while (textureKey.startsWith("#")) {
			textureKey = model.getTexture().get(textureKey.substring(1));
			if (textureKey == null) {
				return IMAGE_ERROR;
			}
			if (loopCount++ > 100) {
				throw new IllegalStateException("Stuck in a loop for 100 cycles when resolving " + textureName + ", stuck at: " + textureKey);
			}
		}
		BufferedImage img = this.textures.get(textureKey);
		if (img == null) {
			return IMAGE_ERROR;
		}
		return img;
	}

}
