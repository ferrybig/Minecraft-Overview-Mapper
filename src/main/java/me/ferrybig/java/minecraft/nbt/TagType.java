/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import me.ferrybig.java.minecraft.nbt.exception.MalformedNBTException;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;
import me.ferrybig.java.minecraft.nbt.exception.UnknownNBTException;

/**
 *
 * @author Fernando
 */
public enum TagType {
	BYTE(1) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new ByteTag(in.readByte());
		}

	},
	SHORT(2) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new ShortTag(in.readShort());
		}

	},
	INT(3) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new IntTag(in.readInt());
		}

	},
	LONG(4) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new LongTag(in.readLong());
		}

	},
	FLOAT(5) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new FloatTag(in.readFloat());
		}

	},
	DOUBLE(6) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			return new DoubleTag(in.readDouble());
		}

	},
	BYTE_ARRAY(7) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			int length = in.readInt();
			if (length < 0) {
				throw new MalformedNBTException("Byte array with a size lower than 0 encountered");
			}
			byte[] array = new byte[length];
			in.readFully(array);
			return new ByteArrayTag(array);
		}

	},
	STRING(8) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {

			int length = in.readShort();
			if (length < 0) {
				throw new MalformedNBTException("String with a size lower than 0 encountered");
			}
			byte[] string = new byte[length];
			in.readFully(string);
			return new StringTag(new String(string, StandardCharsets.UTF_8));
		}

	},
	LIST(9) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			TagType type = getById(in.readByte());
			int length = in.readInt();
			if (length < 0) {
				throw new MalformedNBTException("List with a size lower than 0 encountered");
			}
			if (type == null && length > 0) {
				throw new MalformedNBTException("A list cannot exists of TAG_END tags");
			}
			List<Tag> list = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				list.add(type.read(in));
			}
			return new ListTag(type == null ? BYTE : type, list, false);
		}

	},
	COMPOUND(10) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			Map<String, Tag> tagMap = new HashMap<>();
			while (true) {
				TagType type = getById(in.readByte());
				if (type == null) {
					return new CompoundTag(tagMap, false);
				}
				int length = in.readShort();
				if (length < 0) {
					throw new MalformedNBTException("Tag name with a size lower than 0 encountered");
				}
				byte[] string = new byte[length];
				in.readFully(string);
				tagMap.put(new String(string, StandardCharsets.UTF_8), type.read(in));
			}
		}

	},
	INT_ARRAY(11) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			int length = in.readInt();
			if (length < 0) {
				throw new MalformedNBTException("Int array with a size lower than 0 encountered");
			}
			int[] array = new int[length];
			for (int i = 0; i < length; i++) {
				array[i] = in.readInt();
			}
			return new IntArrayTag(array);
		}

	},
	LONG_ARRAY(12) {
		@Override
		public Tag read(DataInput in) throws IOException, NBTException {
			int length = in.readInt();
			if (length < 0) {
				throw new MalformedNBTException("Long array with a size lower than 0 encountered");
			}
			long[] array = new long[length];
			for (int i = 0; i < length; i++) {
				array[i] = in.readLong();
			}
			return new LongArrayTag(array);
		}

	},;
	private final int id;

	private TagType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public abstract Tag read(DataInput in) throws IOException, NBTException;

	private static final TagType[] values;

	@Nullable
	public static TagType getById(int id) throws UnknownNBTException {
		try {
			return values[id];
		} catch (IndexOutOfBoundsException e) {
			throw new UnknownNBTException(e);
		}
	}

	static {
		values = new TagType[13];
		for (TagType t : values()) {
			values[t.getId()] = t;
		}
	}
}
