/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package minecraft.stream.renderer.streams;

import java.io.Closeable;

/**
 *
 * @author Fernando
 */
public interface ByteCounterInput extends Closeable{
    public long getReadBytes();
    public void resetReadBytes();
    public default long getAndResetReadBytes(){
        long readBytes = getReadBytes();
        resetReadBytes();
        return readBytes;
    }
    
}
