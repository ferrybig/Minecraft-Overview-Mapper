/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import com.flowpowered.nbt.CompoundTag;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import me.ferrybig.java.minecraft.overview.mapper.Base64;

public class SimpleHTMLOutputRenderer extends SimpleRenderer {

	private final File out;
	private Writer writer;
	private final String imageformat;
	private boolean writingStyleSheets = false;

	public SimpleHTMLOutputRenderer(RegionRenderer renderer, File out) {
		this(renderer, out, "gif");
	}

	public SimpleHTMLOutputRenderer(RegionRenderer renderer, File out, String imageformat) {
		super(renderer);
		this.out = out;
		this.imageformat = imageformat;
	}

	private String getID(int x, int z) {
		return "img_" + x + "_" + z;
	}

	@Override
	protected void addLevelDat(CompoundTag level) throws IOException {
		if (writingStyleSheets) {
			writer.append("</style>");
			writingStyleSheets = false;
		}
		writer.append("<title>" + ((CompoundTag) level.getValue().get("Data")).getValue().get("LevelName") + "</title>");
	}

	@Override
	protected void addImage(BufferedImage tile, int x, int z) throws IOException {
		if (!writingStyleSheets) {
			writer.append("<style type=\"text/css\">");
			writingStyleSheets = true;
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream(1024 * 8);
		ImageIO.write(tile, imageformat, stream);
		writer.append("#" + getID(x, z)
			+ " {background:url('data:image/" + imageformat + ";base64,").
			append(Base64.encodeBytes(stream.toByteArray(), 0, stream.size())).
			append("');}\n");
		this.addRegion(this.createRegion(x, z));
	}

	@Override
	public void startRender() throws IOException {
		writer = Files.newBufferedWriter(out.toPath());
		writer.append("<html><head>\n").
			append("<style type=\"text/css\">").
			append(""
				+ "#render{border:4px black inset;background:black url('data:image/png;base64,"
				+ "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAA"
				+ "FklEQVQI12NgYGBwc3NjcHNzY2BgAAALigGlEaF+BwAAAABJRU5ErkJggg==');"
				+ "background-size: 2px;color:white;}\n"
				+ "#render > tr td{font-size:0px;width:1px;height:1px;overflow:visible;border:none}\n"
				+ "#render td div{width:512px;height:512px;display:inline-block;"
				+ "background-size:100%;overflow:hidden;}\n"
				+ "#render td div, td {margin:0px;padding:0px}\n"
				+ "#render .header, #render .footer {margin: 4px;}\n"
			).
			append("</style>\n");
	}

	@Override
	public void finishRender() throws IOException {
		if (writingStyleSheets) {
			writer.append("</style>");
			writingStyleSheets = false;
		}
		writer.append("</head><body>");
		writer.append("<table id=\"render\" cellspacing=\"0\" cellpadding=\"0\">"
			+ "<thead><tr><td class=\"header\" colspan=" + maxX + ">"
			+ "<center>Map renderer created by ferrybig</center></td></tr></thead>\n<tr>");
		int lastZ = minZ;
		int lastX = minX - 1;
		for (Region r : getRegions()) {
			if (lastZ != r.rz) {
				if (lastX != maxX) {
					assert lastX < maxX;
					writer.append("<td colspan=" + (maxX - lastX) + "></td>");
				}
				writer.append("</tr>\n<tr>");
				lastX = minX - 1;

			}
			if ((lastX + 1) != r.rx) {
				assert (lastX + 1) < r.rx;
				writer.append("<td colspan=" + (r.rx - lastX - 1) + "></td>");
			}
			writer.append("<td><div id=" + r.getID() + "></div></td>");
			lastX = r.rx;
			lastZ = r.rz;
		}
		if ((lastX) != maxX) {
			assert (lastX) < maxX;
			writer.append("<td colspan=" + (maxX - lastX) + "></td>");
		}
		writer.append("</tr>\n<tfoot><tr><td class=\"footer\" colspan=" + getRegionSpanX()
			+ "><center>Rendered at: " + new Date().toString() + " | "
			+ "Map renderer created by ferrybig</center></td></tr></tfoot>");
		writer.append("</table></body></html>");
		writer.close();
	}

	private final Set<Region> regions = new TreeSet<>();
	private int minX = 2147483647;
	private int minZ = 2147483647;
	private int maxX = -2147483648;
	private int maxZ = -2147483648;

	private Region regionAt(int x, int z) {
		for (Region r : regions) {
			if (x == r.rx && z == r.rz) {
				return r;
			}
		}
		return null;
	}

	private void addRegion(Region paramRegion) {
		this.regions.add(paramRegion);
		if (paramRegion.rx < this.minX) {
			this.minX = paramRegion.rx;
		}
		if (paramRegion.rz < this.minZ) {
			this.minZ = paramRegion.rz;
		}
		if (paramRegion.rx >= this.maxX) {
			this.maxX = paramRegion.rx;
		}
		if (paramRegion.rz >= this.maxZ) {
			this.maxZ = paramRegion.rz;
		}
	}

	private Region createRegion(int x, int z) {
		Region r = new Region();
		r.rx = x;
		r.rz = z;
		return r;
	}

	private int getRegionSpanX() {
		return Math.max(maxX - minX + 1, 0);
	}

	private int getRegionSpanZ() {
		return Math.max(maxZ - minZ + 1, 0);
	}

	private Set<Region> getRegions() {
		return regions;
	}

	public static class Region implements Comparable<Region> {

		public int rx;
		public int rz;
		public String regionFile;
		public String imageFile;

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 47 * hash + this.rx;
			hash = 47 * hash + this.rz;
			return hash;
		}

		public String getID() {
			return "img_" + rx + "_" + rz;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Region other = (Region) obj;
			if (this.rx != other.rx) {
				return false;
			}
			return this.rz == other.rz;
		}

		@Override
		public int compareTo(Region o) {
			if (this.rz == o.rz) {
				if (this.rx == o.rx) {
					return 0;
				}
				if (this.rx < o.rx) {
					return -1;
				}
				return 1;
			}
			if (this.rz < o.rz) {
				return -1;
			}
			return 1;
		}
	}
}
