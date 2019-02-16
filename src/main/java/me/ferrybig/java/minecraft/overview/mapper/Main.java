/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import me.ferrybig.java.minecraft.overview.mapper.input.DirectoryInputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.TarGzInputSource;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap;
import me.ferrybig.java.minecraft.overview.mapper.render.BlockMap;
import me.ferrybig.java.minecraft.overview.mapper.render.DefaultImageRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.RenderEngine;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleHTMLOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleImageOutputRenderer;
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

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
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

		RegionRenderer rend = new DefaultImageRenderer(BlockMap.loadDefault(), BiomeMap.loadDefault());

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
			outputSource = new SimpleImageOutputRenderer(rend, out);
		}

		System.err.println("In-type: " + inputSource.getClass());
		System.err.println("Out-type: " + outputSource.getClass());
		outputSource.forInputSource(inputSource, s -> System.out.println("Start:\t" + s), s -> System.out.println("End:\t" + s));
	}
}
