/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.io.IOException;
import javax.annotation.Nonnull;

public interface InputSource {

	@Nonnull
	public InputInfo generateFileListing() throws IOException;
}
