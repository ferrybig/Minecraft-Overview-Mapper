/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.streams;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 *
 * @author Fernando
 */
public class ByteCountingDataInputStream extends DataInputStream implements ByteCounterInput {

	private final ByteCounterInput counter;

	public ByteCountingDataInputStream(InputStream in) {
		super(in = new ByteCounterInputStream(in));
		this.counter = (ByteCounterInput) in;
	}

	@Override
	public long getReadBytes() {
		return counter.getReadBytes();
	}

	@Override
	public void resetReadBytes() {
		counter.resetReadBytes();
	}

	@Override
	public long getAndResetReadBytes() {
		return counter.getAndResetReadBytes();
	}
}
