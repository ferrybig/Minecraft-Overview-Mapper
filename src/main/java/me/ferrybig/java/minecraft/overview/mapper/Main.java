/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import me.ferrybig.java.minecraft.overview.mapper.input.DirectoryInputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.TarGzInputSource;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap;
import me.ferrybig.java.minecraft.overview.mapper.render.ComplexImageOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.FlatImageRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RenderEngine;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleHTMLOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) throws IOException, ParseException {
		// create Options object
		Options options = new Options();

		Option input = Option
			.builder("i")
			.argName("target")
			.hasArg()
			.desc("Input file/folder")
			.required()
			.longOpt("input")
			.build();

		Option output = Option
			.builder("o")
			.argName("target")
			.hasArg()
			.desc("Output file/folder")
			.required()
			.longOpt("output")
			.build();

		options.addOption(input);
		options.addOption(output);

		CommandLineParser cmdParser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = cmdParser.parse(options, args);
		} catch (org.apache.commons.cli.MissingOptionException missing) {
			System.err.println("Missing: " + missing.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar file.jar", options);
			System.exit(1);
			return;
		}

		Path in = Paths.get(cmd.getOptionValue(input.getOpt()));
		Path out = Paths.get(cmd.getOptionValue(output.getOpt()));

		System.err.println("In: " + in.toAbsolutePath());
		System.err.println("Out: " + out.toAbsolutePath());

		TextureParser parser = new TextureParser();
		parser.readAll(Arrays.asList(new File("C:\\Users\\Fernando\\AppData\\Roaming\\.minecraft\\versions\\1.13.2\\1.13.2.jar")));

		RegionRenderer rend = new FlatImageRenderer(new TextureCache(parser), BiomeMap.loadDefault());

		InputSource inputSource;
		if (in.toAbsolutePath().toString().endsWith(".tar.gz")) {
			inputSource = new TarGzInputSource(in.toFile());
		} else {
			inputSource = new DirectoryInputSource(in.toFile());
		}
		RenderEngine outputSource;
		if (out.toAbsolutePath().toString().endsWith(".html")) {
			outputSource = new SimpleHTMLOutputRenderer(rend, out.toFile(), "gif");
		} else {
			outputSource = new ComplexImageOutputRenderer(rend, out);
		}

		System.err.println("In-type: " + inputSource.getClass());
		System.err.println("Out-type: " + outputSource.getClass());
		outputSource.forInputSource(inputSource, s -> System.out.println("Start:\t" + s), s -> System.out.println("End:\t" + s));
	}
}
