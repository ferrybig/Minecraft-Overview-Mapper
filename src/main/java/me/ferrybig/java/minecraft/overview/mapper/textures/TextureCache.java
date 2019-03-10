/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;

public class TextureCache {

	private static final int IMAGE_SIZE = 16;
	private static final int IMAGE_SIZE_SQUARED = IMAGE_SIZE * IMAGE_SIZE;
	private static final Color WATER_COLOR = new Color(0, 38, 248, 0);
	private static final Color GRASS_COLOR = new Color(10, 128, 20, 0);

	private final LoadingCache<TextureKey, TextureMapper> cache;

	public TextureCache(TextureParser parser) {
		this.cache = CacheBuilder.newBuilder()
			.maximumSize(256)
			.build(new CacheLoader<TextureKey, TextureMapper>() {
				@Override
				public TextureMapper load(TextureKey key) throws IOException {
					Variant variant = parser.getMaterial(key.getBlock(), key.getState());
					BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = image.createGraphics();
					try {
						Stream<VariantModel> stream;
						if ("minecraft:water".equals(key.getBlock()) || "minecraft:bubble_column".equals(key.getBlock())) {
							stream = parser.getMaterial("render_water", Collections.emptyMap()).stream();
						} else if ("true".equals(key.getState().get("waterlogged"))) {
							stream = Stream.concat(variant.stream(), parser.getMaterial("render_water", Collections.emptyMap()).stream());
						} else {
							stream = variant.stream();
						}

						Iterator<Map.Entry<VariantModel, Cube>> iterator = stream
							.flatMap(e -> e.getModel().getElements().stream().map(c -> (Map.Entry<VariantModel, Cube>) new AbstractMap.SimpleImmutableEntry<>(e, c)))
							.sorted(Comparator.comparing((Map.Entry<VariantModel, Cube> c) -> c.getValue().getFrom().getY()))
							.iterator();
						while (iterator.hasNext()) {
							Map.Entry<VariantModel, Cube> next = iterator.next();
							Cube cube = next.getValue();
							Face face = cube.getUp();
							if (face == null) {
								continue;
							}
							BufferedImage texture = parser.getTexture(next.getKey().getModel(), face.getTexture());
							if (face.usesTintIndex()) {
								if (face.getTintIndex() == 1) { // tintindex of 1 is the special water texture
									BufferedImage newTexture = new BufferedImage(texture.getWidth(), texture.getHeight(), texture.getType());
									tint(texture, newTexture, WATER_COLOR);
									texture = newTexture;
								} else if (face.getTintIndex() == 0) { // tintindex of 0 is biome colors
									BufferedImage newTexture = new BufferedImage(texture.getWidth(), texture.getHeight(), texture.getType());
									tint(texture, newTexture, GRASS_COLOR);
									texture = newTexture;
								}
							}
							boolean draw = g2.drawImage(
								texture,
								// destination
								(int) cube.getFrom().getX(), (int) cube.getFrom().getZ(),
								(int) (cube.getTo().getX() - cube.getFrom().getX()), (int) (cube.getTo().getZ() - cube.getFrom().getZ()),
								// source
								(int) face.getPoint1().getX(), (int) face.getPoint1().getY(),
								(int) (face.getPoint2().getX() - face.getPoint1().getX()), (int) (face.getPoint2().getY() - face.getPoint1().getY()),
								null);
							if (!draw) {
								throw new IllegalStateException("Draw call returned false");
							}
						}
					} finally {
						g2.dispose();
					}
					int transparantPixels = 0;
					int fullyTransparantPixels = 0;
					int[] pixels = image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
					for (int pixel : pixels) {
						int aplha = (pixel >>> 24);
						if (aplha != 0xff) {
							transparantPixels++;
							if (aplha == 0) {
								fullyTransparantPixels++;
							}
						}
					}
					assert transparantPixels <= IMAGE_SIZE_SQUARED;
					assert fullyTransparantPixels <= transparantPixels;
					assert 0 <= fullyTransparantPixels;
					boolean isFullyTransparant = fullyTransparantPixels == IMAGE_SIZE_SQUARED;
					boolean containsTransparancy = transparantPixels != 0;

					if (isFullyTransparant) {
						return EmptyTextureMapper.INSTANCE;
					} else if (containsTransparancy) {
						return new TransparantTextureMapper(image, key.getBlock());
					} else {
						return new SolidTextureMapper(image, key.getBlock());
					}
				}

			});
	}

	public TextureMapper get(String block, Map<String, String> state) throws ExecutionException {
		return this.cache.get(new TextureKey(block, state));
	}

	private static void tint(BufferedImage source, BufferedImage dst, Color color) {
		int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
		for (int i = 0; i < pixels.length; i++) {
			Color pixelColor = new Color(pixels[i], true);
			int r = (pixelColor.getRed() + color.getRed()) / 2;
			int g = (pixelColor.getGreen() + color.getGreen()) / 2;
			int b = (pixelColor.getBlue() + color.getBlue()) / 2;
			int a = pixelColor.getAlpha();
			pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		dst.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
	}

	public interface TextureMapper {

		boolean isOpaque();

		void apply(int[] dstPixels);

		String getBlock();
	}

	private static class EmptyTextureMapper implements TextureMapper {

		public static final EmptyTextureMapper INSTANCE = new EmptyTextureMapper();

		@Override
		public void apply(int[] dstPixels) {
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public String toString() {
			return "EmptyTextureMapper{" + '}';
		}

		@Override
		public String getBlock() {
			return "special:empty";
		}

	}

	private static class TransparantTextureMapper implements TextureMapper {

		private final int[] srcPixels;
		private final String block;

		public TransparantTextureMapper(BufferedImage image, String block) {
			this.srcPixels = image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			this.block = block;
		}

		@Override
		public void apply(int[] dstPixels) {
			for (int i = 0; i < srcPixels.length; i++) {
				int aplha = (srcPixels[i] >>> 24);
				if (aplha == 0xff) {
					dstPixels[i] = srcPixels[i];
				} else if (aplha != 0) {
					int c1 = dstPixels[i];
					int c2 = srcPixels[i];

					int c1red = (c1 >> 16) & 0xff;
					int c1green = (c1 >> 8) & 0xff;
					int c1blue = (c1 >> 0) & 0xff;

					int c2red = (c2 >> 16) & 0xff;
					int c2green = (c2 >> 8) & 0xff;
					int c2blue = (c2 >> 0) & 0xff;

					float factor = aplha / 256f;
					int red = (int) (c1red * (1 - factor) + c2red * factor);
					int green = (int) (c1green * (1 - factor) + c2green * factor);
					int blue = (int) (c1blue * (1 - factor) + c2blue * factor);
					dstPixels[i] = (red << 16) | (green << 8) | blue;
				}
			}
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public String getBlock() {
			return block;
		}

		@Override
		public String toString() {
			return "TransparantTextureMapper{" + "block=" + block + '}';
		}

	}

	private static class SolidTextureMapper implements TextureMapper {

		private final int[] srcPixels;
		private final String block;

		public SolidTextureMapper(BufferedImage image, String block) {
			this.srcPixels = image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			this.block = block;
		}

		@Override
		public void apply(int[] dstPixels) {
			System.arraycopy(this.srcPixels, 0, dstPixels, 0, this.srcPixels.length);
		}

		@Override
		public boolean isOpaque() {
			return true;
		}

		@Override
		public String getBlock() {
			return block;
		}

		@Override
		public String toString() {
			return "SolidTextureMapper{" + "block=" + block + '}';
		}

	}

}
