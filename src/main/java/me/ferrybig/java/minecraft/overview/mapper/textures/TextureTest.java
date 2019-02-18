/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.textures;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.Variant;
import me.ferrybig.java.minecraft.overview.mapper.textures.variant.VariantModel;

public class TextureTest {

	public static void main(String... args) throws IOException, InterruptedException {
		TextureParser parser = new TextureParser();
		parser.readAll(Arrays.asList(new File("C:\\Users\\Fernando\\AppData\\Roaming\\.minecraft\\versions\\1.13.2\\1.13.2.jar")));
		Thread.sleep(100);
		Variant bedrock = parser.getMaterial("bedrock").resolve(Collections.emptyMap());
		System.err.println("bedrock");
		System.err.println(bedrock);
		renderTestNorth(parser, bedrock, "renderTest/bedrockNorth.png");
		renderTestUp(parser, bedrock, "renderTest/bedrockUp.png");

		Variant torch = parser.getMaterial("torch").resolve(Collections.emptyMap());
		System.err.println("----------------------------------");
		System.err.println("----------------------------------");
		System.err.println("torch");
		System.err.println(torch);
		renderTestNorth(parser, torch, "renderTest/torchNorth.png");
		renderTestUp(parser, torch, "renderTest/torchUp.png");

		Map<String, String> state = new HashMap<>();
		state.put("east", "true");
		state.put("west", "true");
		state.put("north", "true");
		state.put("south", "true");
		state.put("waterlogged", "false");
		Variant oakFence = parser.getMaterial("oak_fence").resolve(state);
		System.err.println("----------------------------------");
		System.err.println("----------------------------------");
		System.err.println("oak_fence");
		System.err.println(oakFence);
		renderTestNorth(parser, oakFence, "renderTest/oakFenceNorth.png");
		renderTestUp(parser, oakFence, "renderTest/oakFenceUp.png");

	}

	private static void renderTestNorth(TextureParser parser, Variant variant, String out) throws IOException {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			Iterator<Map.Entry<VariantModel, Cube>> iterator = variant
				.stream()
				.flatMap(e -> e.getModel().getElements().stream().map(c -> (Map.Entry<VariantModel, Cube>) new AbstractMap.SimpleImmutableEntry<>(e, c)))
				.sorted(Comparator.comparing((Map.Entry<VariantModel, Cube> c) -> c.getValue().getFrom().getZ()))
				.iterator();
			while (iterator.hasNext()) {
				Map.Entry<VariantModel, Cube> next = iterator.next();
				Cube cube = next.getValue();
				Face face = cube.getNorth();
				if (face == null) {
					System.err.println("Skipping cube: " + cube);
					continue;
				}
				System.err.println("Render cube: " + cube);
				boolean draw = g2.drawImage(
					parser.getTexture(next.getKey().getModel(), face.getTexture()),
					// destination
					(int) cube.getFrom().getX(), (int) cube.getFrom().getY(),
					(int) (cube.getTo().getX() - cube.getFrom().getX()), (int) (cube.getTo().getY() - cube.getFrom().getY()),
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
		ImageIO.write(image, "png", new File(out));
	}

	private static void renderTestUp(TextureParser parser, Variant variant, String out) throws IOException {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
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
					System.err.println("Skipping cube: " + cube);
					continue;
				}
				System.err.println("Render cube: " + cube);
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
		ImageIO.write(image, "png", new File(out));
	}
}
