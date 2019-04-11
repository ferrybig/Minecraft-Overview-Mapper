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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;

public class TextureCache {

	private static final int IMAGE_SIZE = 16;
	private static final int IMAGE_SIZE_SQUARED = IMAGE_SIZE * IMAGE_SIZE;
	@Nonnull
	private final BiomeMap biomes;
	@Nonnull
	private final LoadingCache<TextureKey, TextureMapper> cache;
	@Nonnull
	private final TextureParser parser;

	public TextureCache(@Nonnull TextureParser parser, @Nonnull BiomeMap biomes) {
		this.parser = Objects.requireNonNull(parser, "parser");
		this.biomes = Objects.requireNonNull(biomes, "biomes");
		this.cache = CacheBuilder.newBuilder()
			.maximumSize(256)
			.build(new CacheLoader<TextureKey, TextureMapper>() {
				@Override
				public TextureMapper load(TextureKey key) throws IOException {
					return TextureCache.this.load(key);
				}
			});
	}

	@Nonnull
	private TextureMapper load(@Nonnull TextureKey key) throws IOException {
		Variant variant = this.parser.getMaterial(key);
		BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			Stream<VariantModel> stream;
			if ("minecraft:water".equals(key.getBlock()) || "minecraft:bubble_column".equals(key.getBlock())) {
				stream = this.parser.getMaterial(RENDER_WATER, Collections.emptyMap()).stream();
			} else if ("true".equals(key.getState().get("waterlogged"))) {
				stream = Stream.concat(variant.stream(), this.parser.getMaterial(RENDER_WATER, Collections.emptyMap()).stream());
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
				BiomeMapId biomeMapId = key.getBiomeMapId();
				if (face.usesTintIndex() && biomeMapId != null) {
					texture = tint(texture, new Color(biomes.getBiomeColor(key.getBiomeId(), biomeMapId)));
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
		boolean heightMapCutOff = fullyTransparantPixels == 0;

		if (isFullyTransparant) {
			return EmptyTextureMapper.INSTANCE;
		} else if (containsTransparancy) {
			return new TransparantTextureMapper(image, key.getBlock(), heightMapCutOff);
		} else {
			return new SolidTextureMapper(image, key.getBlock());
		}
	}
	private static final String RENDER_WATER = "render_water";

	@Nonnull
	public TextureMapper get(@Nonnull String block, @Nonnull Map<String, String> state, int biomeId) throws ExecutionException {
		return this.cache.get(TextureKey.of(block, state, biomeId));
	}

	@Nonnull
	public TextureMapper get(@Nonnull TextureKey key) throws ExecutionException {
		return this.cache.get(key);
	}

	@Nonnull
	private static BufferedImage tint(BufferedImage source, Color color) {
		BufferedImage dst = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		tint(source, dst, color);
		return dst;
	}

	private static void tint(@Nonnull BufferedImage source, @Nonnull BufferedImage dst, @Nonnull Color color) {
		int[] pixels;
		if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
			pixels = new int[source.getWidth() * source.getHeight()];
			source.getRaster().getDataElements(0, 0, source.getWidth(), source.getHeight(), pixels);
		} else {
			pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
		}
		for (int i = 0; i < pixels.length; i++) {
			Color pixelColor = new Color(pixels[i], true);
			int r = (pixelColor.getRed() + color.getRed()) / 2;
			int g = (pixelColor.getGreen() + color.getGreen()) / 2;
			int b = (pixelColor.getBlue() + color.getBlue()) / 2;
			int a = pixelColor.getAlpha();
			pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		if (dst.getType() == BufferedImage.TYPE_INT_ARGB) {
			dst.getRaster().setDataElements(0, 0, source.getWidth(), source.getHeight(), pixels);
		} else {
			dst.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
		}
	}

	public interface TextureMapper {

		boolean isOpaque();

		boolean cutOffHeightMap();

		void apply(@Nonnull int[] dstPixels);

		@Nonnull
		String getBlock();
	}

	private static class EmptyTextureMapper implements TextureMapper {

		public static final EmptyTextureMapper INSTANCE = new EmptyTextureMapper();

		@Override
		public void apply(@Nonnull int[] dstPixels) {
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public boolean cutOffHeightMap() {
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

		private final boolean heightMapCutOff;
		@Nonnull
		private final int[] srcPixels;
		private final String block;

		public TransparantTextureMapper(@Nonnull BufferedImage image, @Nonnull String block, boolean heightMapCutOff) {
			this.srcPixels = image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			this.block = block;
			this.heightMapCutOff = heightMapCutOff;
		}

		@Override
		public boolean cutOffHeightMap() {
			return heightMapCutOff;
		}

		@Override
		public void apply(@Nonnull int[] dstPixels) {
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
					dstPixels[i] = (0xff << 24) | (red << 16) | (green << 8) | blue;
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

		public SolidTextureMapper(@Nonnull BufferedImage image, @Nonnull String block) {
			this.srcPixels = image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			this.block = block;
		}

		@Override
		public boolean cutOffHeightMap() {
			return true;
		}

		@Override
		public void apply(@Nonnull int[] dstPixels) {
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
