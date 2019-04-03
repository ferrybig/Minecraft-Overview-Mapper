/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class WorldFile {

	private static final UUID NOT_KNOWN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private static final String NOT_KNOWN_STRUCTURE_NAME = "Unknown structure";
	private static final int NOT_KNOWN_DIMENSION = Integer.MIN_VALUE;
	private static final int NOT_KNOWN_LOCATION = Integer.MIN_VALUE;
	private static final Type[] KNOWN_TYPES = Type.values();

	@Nonnull
	private final Type type;
	private final int x;
	private final int z;
	@Nonnull
	private final UUID uuid;
	private final int dimension;
	@Nonnull
	private final String orignalName;
	@Nonnull
	private final String structureName;

	private WorldFile(Type type, int x, int z, UUID uuid, int dimension, String orignalName, String structureName) {
		this.type = type;
		this.x = x;
		this.z = z;
		this.uuid = uuid;
		this.dimension = dimension;
		this.orignalName = orignalName;
		this.structureName = structureName;
	}

	@Nonnull
	public static WorldFile of(@Nonnull String name) {
		for (Type type : KNOWN_TYPES) {
			Matcher matcher = type.getPattern().matcher(name);
			if (matcher.matches()) {
				return type.makeWorldFile(name, matcher);
			}
		}
		throw new AssertionError(name);
	}

	public Type getType() {
		return type;
	}

	public int getX() {
		if (!type.hasLocation()) {
			throw new IllegalStateException(this.getOrignalName() + " has no location component");
		}
		return x;
	}

	public int getZ() {
		if (!type.hasLocation()) {
			throw new IllegalStateException(this.getOrignalName() + " has no location component");
		}
		return z;
	}

	public UUID getUuid() {
		if (!type.hasUUID()) {
			throw new IllegalStateException(this.getOrignalName() + " has no uuid component");
		}
		return uuid;
	}

	public int getDimension() {
		if (!type.hasLocation()) {
			throw new IllegalStateException(this.getOrignalName() + " has no dimension component");
		}
		return dimension;
	}

	public String getOrignalName() {
		return orignalName;
	}

	public String getStructureName() {
		if (!type.hasStructureName()) {
			throw new IllegalStateException(this.getOrignalName() + " has no structure name component");
		}
		return structureName;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 59 * hash + Objects.hashCode(this.orignalName);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final WorldFile other = (WorldFile) obj;
		if (!Objects.equals(this.orignalName, other.orignalName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("WorldFile{name=");
		b.append(this.getOrignalName());
		b.append(",type=");
		b.append(this.getType());
		if (this.type.hasDimension()) {
			b.append(",dimension=");
			b.append(this.getDimension());
		}
		if (this.type.hasLocation()) {
			b.append(",x=");
			b.append(this.getX());
			b.append(",z=");
			b.append(this.getZ());
		}
		if (this.type.hasUUID()) {
			b.append(",uuid=");
			b.append(this.getUuid());
		}
		if (this.type.hasStructureName()) {
			b.append(",structureName=");
			b.append(this.getStructureName());
		}
		b.append("}");
		return b.toString();
	}

	public enum Type {
		/**
		 * Pattern that matches anvil region files. Examples:
		 * <ul>
		 * <li><code>region/r.-1.-3.mca</code></li>
		 * <li><code>region/r.1.-3.mca</code></li>
		 * <li><code>DIM-1/region/r.1673.-334.mca</code></li>
		 * <li><code>DIM1/region/r.-73.54.mca</code></li>
		 * </ul>
		 */
		REGION_MCA(true, false, true, false, "^(?:[^/]*/)*?(?:DIM(-?\\d+)/)?region/r\\.(-?\\d+)\\.(-?\\d+)\\.mca$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher localMatcher) {
				return new WorldFile(
					this,
					Integer.parseInt(localMatcher.group(2)),
					Integer.parseInt(localMatcher.group(3)),
					NOT_KNOWN_UUID,
					localMatcher.group(1) == null ? 0 : Integer.parseInt(localMatcher.group(1)),
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		REGION_MCR(true, false, true, false, "^(?:[^/]*/)*?(?:DIM(-?\\d+)/)?region/r\\.(-?\\d+)\\.(-?\\d+)\\.mcr$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)),
					NOT_KNOWN_UUID,
					matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1)),
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		PLAYER_DATA(false, true, false, false, "^(?:[^/]*/)*playerdata/([\\w]{8}(-[\\w]{4}){3}-[\\w]{12}).dat$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					UUID.fromString(matcher.group(1)),
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}
		},
		LEVEL_DAT(false, false, false, false, "^(?:[^/]*/)*level.dat$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		LEVEL_DAT_BAK(false, false, false, false, "^(?:[^/]*/)*level.dat_old$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		STATS(false, true, false, false, "^(?:[^/]*/)*stats/([\\w]{8}(-[\\w]{4}){3}-[\\w]{12}).json$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					UUID.fromString(matcher.group(1)),
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		ADVANCEMENTS(false, true, false, false, "^(?:[^/]*/)*advancements/([\\w]{8}(-[\\w]{4}){3}-[\\w]{12}).json$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					UUID.fromString(matcher.group(1)),
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		STRUCTURE(true, false, false, true, "^(?:[^/]*/)*(?:DIM(-?\\d+)/)?data/(EndCity|Fortress|Mansion|Mineshaft|Monument|Stronghold|Temple|Village).dat") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					matcher.group(1)
				);
			}

		},
		ICON(false, false, false, false, "^(?:[^/]*/)*icon.png$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		LOCK(false, false, false, false, "^(?:[^/]*/)*session.lock$") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},
		UNKNOWN_FILE(false, false, false, false, ".*") {
			@Override
			WorldFile makeWorldFile(String fileName, Matcher matcher) {
				return new WorldFile(
					this,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_LOCATION,
					NOT_KNOWN_UUID,
					NOT_KNOWN_DIMENSION,
					fileName,
					NOT_KNOWN_STRUCTURE_NAME
				);
			}

		},;
		private final boolean hasDimension;
		private final boolean hasUUID;
		private final boolean hasLocation;
		private final boolean hasStructureName;
		private final Pattern pattern;

		private Type(boolean hasDimension, boolean hasUUID, boolean hasLocation, boolean hasStructureName, String pattern) {
			this.hasDimension = hasDimension;
			this.hasUUID = hasUUID;
			this.hasLocation = hasLocation;
			this.hasStructureName = hasStructureName;
			this.pattern = Pattern.compile(pattern);
		}

		public boolean hasDimension() {
			return hasDimension;
		}

		public boolean hasUUID() {
			return hasUUID;
		}

		public boolean hasLocation() {
			return hasLocation;
		}

		public boolean hasStructureName() {
			return hasStructureName;
		}

		@Nonnull
		Pattern getPattern() {
			return pattern;
		}

		@Nonnull
		abstract WorldFile makeWorldFile(String fileName, Matcher matcher);

	}
}
