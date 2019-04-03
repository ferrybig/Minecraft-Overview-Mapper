/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.nbt;

import java.io.DataInputStream;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Fernando
 */
public class TagTest {
@Test
	public void testReading() throws Exception {
		Tag tag;
		try(DataInputStream in = new DataInputStream(new GZIPInputStream(getClass().getResourceAsStream("bigtest.nbt")))) {
			tag = Tag.fromNbt(in);
		}
		assertEquals(tag.resolve("nested compound test", "egg", "name").asStringTag().get(), "Eggbert");
	}

}
