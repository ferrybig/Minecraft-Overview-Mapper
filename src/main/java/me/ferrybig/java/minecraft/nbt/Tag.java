/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static me.ferrybig.java.minecraft.nbt.TagType.getById;
import me.ferrybig.java.minecraft.nbt.exception.MalformedNBTException;
import me.ferrybig.java.minecraft.nbt.exception.NBTException;

public abstract class Tag extends Resolveable {

	protected final TagType type;

	public Tag(TagType type) {
		this.type = type;
	}

	public TagType getType() {
		return type;
	}

	@Nonnull
	@Override
	public Tag asTag() {
		return this;
	}

	@Nonnull
	public abstract Object get();

	public static Tag fromNbt(DataInput in) throws IOException, NBTException {
		TagType type = getById(in.readByte());
		if(type == null) {
			throw new MalformedNBTException("File consists of an TAG_END only");
		}
		int length = in.readShort();
		if(length < 0) {
			throw new MalformedNBTException("Tag name with a size lower than 0 encountered");
		}
		in.skipBytes(length);
		return type.read(in);
	}

}
