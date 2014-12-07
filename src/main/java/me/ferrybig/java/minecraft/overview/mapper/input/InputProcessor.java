/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.IOException;

public interface InputProcessor {
    public void registerListener(InputNotifer listener);
    
    public void unRegisterListener(InputNotifer listener);
    
    public void start() throws IOException;
}
