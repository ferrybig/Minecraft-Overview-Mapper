/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.streams;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Fernando
 */
public class ByteCounterInputStream extends FilterInputStream implements ByteCounterInput {

    private long readBytes = 0;
    private long readBytesWithoutMark = 0;

    public ByteCounterInputStream(InputStream in) {
        super(in);
    }

    private void incrementBytes(long bytes) {
        if (bytes >= 0) { // Branche predictor should optimalise this very well
            readBytes += bytes;
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
        readBytes = readBytesWithoutMark;
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        readBytesWithoutMark = readBytes;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        incrementBytes(skipped);
        return skipped;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        incrementBytes(read);
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = in.read(b);
        incrementBytes(read);
        return read;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) {
            incrementBytes(1);
        }
        return read;
    }

    @Override
    public long getReadBytes() {
        return this.readBytes;
    }

    @Override
    public void resetReadBytes() {
        this.readBytesWithoutMark = this.readBytesWithoutMark - this.readBytes;
        this.readBytes = 0;
    }

    @Override
    public long getAndResetReadBytes() {
        long bytes = this.getReadBytes();
        resetReadBytes();
        return bytes;
    }
}
