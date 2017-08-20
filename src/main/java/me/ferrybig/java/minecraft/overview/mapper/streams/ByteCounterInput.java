/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.streams;

import java.io.Closeable;

/**
 *
 * @author Fernando
 */
public interface ByteCounterInput extends Closeable {

	public long getReadBytes();

	public void resetReadBytes();

	public long getAndResetReadBytes();

}
