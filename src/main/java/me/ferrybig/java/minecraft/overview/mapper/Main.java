/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import me.ferrybig.java.minecraft.overview.mapper.engine.AbstractProgressReporter;
import me.ferrybig.java.minecraft.overview.mapper.engine.ProgressReporter;
import me.ferrybig.java.minecraft.overview.mapper.engine.RenderEngine;
import me.ferrybig.java.minecraft.overview.mapper.engine.RenderException;
import me.ferrybig.java.minecraft.overview.mapper.engine.RenderOptions;
import me.ferrybig.java.minecraft.overview.mapper.input.ArchieveInputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.DirectoryInputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.InputSource;
import me.ferrybig.java.minecraft.overview.mapper.input.WorldFile;
import me.ferrybig.java.minecraft.overview.mapper.render.BiomeMap;
import me.ferrybig.java.minecraft.overview.mapper.render.ComplexImageOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.FlatImageRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.ImageWriter;
import me.ferrybig.java.minecraft.overview.mapper.render.RegionRenderer;
import me.ferrybig.java.minecraft.overview.mapper.render.SimpleHTMLOutputRenderer;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureCache;
import me.ferrybig.java.minecraft.overview.mapper.textures.TextureParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	private static final Pattern MINECRAFT_RELEASE_VERSION = Pattern.compile("^(\\d*)\\.(\\d*)(?:\\.(\\d*))$");

	public static Optional<Path> getMinecraftDirectory() {
		String userHome = System.getProperty("user.home", ".");
		String applicationData = System.getenv("APPDATA");
		for (Path p : new Path[]{
			Paths.get(userHome, ".minecraft", "versions"),
			Paths.get(applicationData == null ? userHome : applicationData, ".minecraft", "versions"),
			Paths.get(userHome, "Library", "Application Support", "minecraft", "versions"),
			Paths.get(userHome, "minecraft", "versions")
		}) {
			if (Files.exists(p)) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}

	@Nonnull
	public static Optional<Path> existsOrNullPath(Path path) {
		if (Files.exists(path)) {
			return Optional.of(path);
		}
		return Optional.empty();
	}

	private static Optional<Path> getLatestMinecraftVersion(Path versions) throws IOException {
		Predicate<String> minecraftVersion = MINECRAFT_RELEASE_VERSION.asPredicate();
		Optional<Path> result = Files
			.list(versions)
			.filter(path -> minecraftVersion.test(path.getFileName().toString()))
			.sorted(new MinecraftVersionsComparator())
			.findFirst()
			.flatMap(p -> getNamedMinecraftVersion(versions, p.getFileName().toString()));
		return result;
	}

	private static Optional<Path> getNamedMinecraftVersion(Path versions, String name) {
		Path p = versions.resolve(name).resolve(name + ".jar");
		if (!Files.exists(p)) {
			return Optional.empty();
		}
		return Optional.of(p);
	}

	public static List<Path> locateTexturePacks(@Nullable String args, @Nullable String minecraftDir) throws IOException {
		Optional<Path> minecraftVersionsLocation = minecraftDir == null
			? getMinecraftDirectory()
			: existsOrNullPath(Paths.get(minecraftDir, "versions"));
		if (args == null) {
			if (minecraftVersionsLocation.isPresent()) {
				Optional<Path> latestVersion = getLatestMinecraftVersion(minecraftVersionsLocation.get());
				if (latestVersion.isPresent()) {
					return Collections.singletonList(latestVersion.get());
				} else {
					throw new IOException("Could not figure out latestminecraft version, could not figure out latest version");
				}
			} else {
				throw new IOException("Could not figure out latestminecraft version, could not locate minecraft directory");
			}
		} else {
			String[] pathArgs = args.split(File.pathSeparator);
			List<Path> list = new ArrayList<>(pathArgs.length);
			IOException exception = null;
			for (String arg : pathArgs) {
				Path newPath = Paths.get(arg);
				if (!Files.exists(newPath)) {
					FileNotFoundException ex = new FileNotFoundException(newPath.toString());
					if (exception == null) {
						exception = ex;
					} else {
						exception.addSuppressed(ex);
					}
				} else {
					list.add(newPath);
				}
			}
			if (exception != null) {
				throw exception;
			}
			return list;
		}
	}

	public static RenderOptions makeRenderOptions(ProgressReporter progressReporter, RegionRenderer renderer, String inArg, String outArg) {
		System.out.println("Initizing input system");
		Path in = Paths.get(inArg);
		InputSource inputSource;
		if (in.toAbsolutePath().toString().endsWith(".tar.gz")) {
			inputSource = new ArchieveInputSource(in.toFile());
		} else {
			inputSource = new DirectoryInputSource(in);
		}

		System.out.println("Initizing output system");
		ImageWriter outputSource;
		Path out = Paths.get(outArg);
		if (out.toAbsolutePath().toString().endsWith(".html")) {
			outputSource = new SimpleHTMLOutputRenderer(out.toFile(), "gif");
		} else {
			outputSource = new ComplexImageOutputRenderer(out);
		}
		return new RenderOptions(inputSource, renderer, outputSource, progressReporter);
	}

	@Nullable
	public static ProgramOptions parse(String[] args, ProgressReporter progressReporter) throws IOException {
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

		Option parallel = Option
			.builder("p")
			.argName("parallel")
			.desc("The amount of threads this render should use")
			.longOpt("parallel")
			.hasArg()
			.build();

		Option sequentional = Option
			.builder("s")
			.argName("sequentional")
			.desc("Only use a single thread when rendering")
			.longOpt("sequentional")
			.build();

		Option minecraftDirectory = Option
			.builder("m")
			.argName("minecraft-directory")
			.desc("Directory where minecraft files can be found")
			.longOpt("minecraft-directory")
			.build();

		Option texturePacks = Option
			.builder("t")
			.argName("textures")
			.desc("List of texture paths to texture packs, seperated by " + File.pathSeparator)
			.longOpt("textures")
			.build();

		options.addOption(input);
		options.addOption(output);
		options.addOption(sequentional);
		options.addOption(parallel);
		options.addOption(minecraftDirectory);
		options.addOption(texturePacks);

		CommandLineParser cmdParser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = cmdParser.parse(options, args);
		} catch (MissingOptionException missing) {
			System.err.println("Missing: " + missing.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar file.jar", options);
			return null;
		} catch (ParseException ex) {
			System.err.println("Exception during parsing of the command line: " + ex.toString());
			ex.printStackTrace();
			return null;
		}

		System.out.println("Loading textures...");
		TextureParser parser = new TextureParser();
		final List<Path> locateTexturePacks = locateTexturePacks(
			cmd.getOptionValue(texturePacks.getOpt()),
			cmd.getOptionValue(minecraftDirectory.getOpt())
		);
		locateTexturePacks.forEach(System.out::println);
		parser.readAllPaths(locateTexturePacks);
		final TextureCache textureCache = new TextureCache(parser, BiomeMap.loadDefault());

		System.out.println("Initizing rendering system");
		RegionRenderer renderer = new FlatImageRenderer(textureCache);

		System.out.println("Preparing engine...");
		final List<RenderOptions> renderOptions = Collections.singletonList(makeRenderOptions(
			progressReporter,
			renderer,
			cmd.getOptionValue(input.getOpt()),
			cmd.getOptionValue(output.getOpt())
		));

		RenderEngine engine;
		if (cmd.hasOption(sequentional.getOpt())) {
			engine = RenderEngine.sequential();
		} else {
			String val = cmd.getOptionValue(parallel.getOpt());
			engine = RenderEngine.parellel(val == null ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(val));
		}
		return new ProgramOptions(renderOptions, engine);
	}

	public static void main(String[] args) throws IOException, ParseException, NBTException, RenderException, CancellationException, InterruptedException {
		ProgramOptions options = parse(args, new AbstractProgressReporter() {
			@Override
			public void onFileStart(WorldFile file) {
				System.out.println("Start:\t" + file.getOrignalName());
			}

			@Override
			public void onFileEnd(WorldFile file) {
				System.out.println("End:\t" + file.getOrignalName());
			}

			@Override
			public void onProgress(double progress, int processed, int total) {
				System.out.println("Progress:\t" + progress + "%");
			}

		});
		if (options == null) {
			return;
		}

		try (RenderEngine engine = options.getEngine()) {
			int jobId = 0;
			Iterator<RenderOptions> itr = options.getJobs().iterator();
			while (itr.hasNext()) {
				RenderOptions next = itr.next();
				System.out.println("[" + jobId + "] Job:");
				System.out.println("[" + jobId + "] Input: " + next.getFiles());
				System.out.println("[" + jobId + "] Renderer: " + next.getRenderer());
				System.out.println("[" + jobId + "] Output: " + next.getImageWriter());
				System.out.println("[" + jobId + "] Starting render...");
				engine.runJob(next);
				System.out.println("[" + jobId + "] Done!");
				jobId++;
			}
		}

	}

	private static class MinecraftVersionsComparator implements Comparator<Path> {

		@Override
		public int compare(Path a, Path b) {
			Matcher matcherA = MINECRAFT_RELEASE_VERSION.matcher(a.getFileName().toString());
			Matcher matcherB = MINECRAFT_RELEASE_VERSION.matcher(b.getFileName().toString());
			matcherA.matches();
			matcherB.matches();
			int a1 = Integer.parseInt(matcherA.group(1));
			int a2 = Integer.parseInt(matcherA.group(2));
			int a3 = matcherA.group(3) == null ? 0 : Integer.parseInt(matcherA.group(3));
			int b1 = Integer.parseInt(matcherB.group(1));
			int b2 = Integer.parseInt(matcherB.group(2));
			int b3 = matcherB.group(3) == null ? 0 : Integer.parseInt(matcherB.group(3));
			if (a1 > b1) {
				return -1;
			}
			if (a1 < b1) {
				return 1;
			}
			if (a2 > b2) {
				return -1;
			}
			if (a2 < b2) {
				return 1;
			}
			if (a3 > b3) {
				return -1;
			}
			if (a3 < b3) {
				return 1;
			}
			return 0;
		}
	}
}
