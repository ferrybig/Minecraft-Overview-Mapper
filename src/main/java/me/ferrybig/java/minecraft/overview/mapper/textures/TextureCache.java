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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;

public class TextureCache {

	private static final int IMAGE_SIZE = 16;
	private static final int IMAGE_SIZE_SQUARED = IMAGE_SIZE * IMAGE_SIZE;

	private final LoadingCache<TextureKey, TextureMapper> cache;

	public TextureCache(TextureParser parser) {
		this.cache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.build(new CacheLoader<TextureKey, TextureMapper>() {
				@Override
				public TextureMapper load(TextureKey key) throws IOException {
					Variant variant = parser.getMaterial(key.getBlock(), key.getState());
					BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = image.createGraphics();
					try {
						Iterator<Map.Entry<VariantModel, Cube>> iterator = variant
							.stream()
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
							boolean draw = g2.drawImage(
								parser.getTexture(next.getKey().getModel(), face.getTexture()),
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
					System.out.println("Block " + key.getBlock() + " is transparant? " + containsTransparancy + " " + isFullyTransparant);

					try (OutputStream out = Files.newOutputStream(Paths.get("renderTest/" + key.getBlock().replace(":", "_") + ".png"))) {
						ImageIO.write(image, "png", out);
					}
					if (isFullyTransparant) {
						return EmptyTextureMapper.INSTANCE;
					} else {
						return new DefaultTextureMapper(image, !containsTransparancy, key.getBlock());
					}
				}

			});
	}

	public TextureMapper get(String block, Map<String, String> state) throws ExecutionException {
		return this.cache.get(new TextureKey(block, state));
	}


	public interface TextureMapper {

		boolean isOpaque();

		void apply(BufferedImage image, int x, int z);

		String getBlock();
	}

	private static class EmptyTextureMapper implements TextureMapper {

		public static final EmptyTextureMapper INSTANCE = new EmptyTextureMapper();

		private EmptyTextureMapper() {
		}

		@Override
		public void apply(BufferedImage image, int x, int z) {
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
			return "air";
		}

	}

	private static class DefaultTextureMapper implements TextureMapper {

		private final BufferedImage image;
		private final boolean opaque;
		private final String block;

		public DefaultTextureMapper(BufferedImage image, boolean opaque, String block) {
			this.image = image;
			this.opaque = opaque;
			this.block = block;
		}

		@Override
		public void apply(BufferedImage image, int x, int z) {
			int[] srcPixels = this.image.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			int[] dstPixels = image.getRGB(x * IMAGE_SIZE, z * IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, null, 0, IMAGE_SIZE);
			for(int i = 0; i < srcPixels.length; i++) {
				int aplha = (srcPixels[i] >>> 24);
				if (aplha == 0xff) {
					dstPixels[i] = srcPixels[i];
				} else if (aplha != 0) {
					Color color1 = new Color(dstPixels[i]);
					Color color2 = new Color(srcPixels[i]);
					float factor = aplha / 256f;
					int red = (int) (color1.getRed() * (1 - factor) + color2.getRed() * factor);
					int green = (int) (color1.getGreen() * (1 - factor) + color2.getGreen() * factor);
					int blue = (int) (color1.getBlue() * (1 - factor) + color2.getBlue() * factor);
					dstPixels[i] = new Color(red, green, blue).getRGB();
				}
			}
			image.setRGB(x * IMAGE_SIZE, z * IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE, dstPixels, 0, IMAGE_SIZE);
		}

		@Override
		public boolean isOpaque() {
			return this.opaque;
		}

		@Override
		public String getBlock() {
			return block;
		}

		@Override
		public String toString() {
			return "DefaultTextureMapper{" + "block=" + block + ", image=" + image + ", opaque=" + opaque + '}';
		}

	}

}
