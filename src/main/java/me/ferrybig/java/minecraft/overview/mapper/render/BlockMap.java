/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.render;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Fernando
 */
public class BlockMap {

    public static final int INDEX_MASK = 65535;
    public static final int SIZE = 65536;
    public static final int INF_NONE = 0;
    public static final int INF_GRASS = 1;
    public static final int INF_FOLIAGE = 2;
    public static final int INF_WATER = 3;
    public final Block[] blocks;
    public BlockMap(Block[] paramArrayOfBlock) {
        if (paramArrayOfBlock == null) {
            throw new IllegalArgumentException("Blocklist Empty");
        }
        if (paramArrayOfBlock.length != 65536) {
            throw new IllegalArgumentException("Blocklist incorrect size");
        }

        this.blocks = paramArrayOfBlock;
    }

    public static BlockMap load(BufferedReader paramBufferedReader, String paramString) throws IOException {
        Block[] blockColors = new Block[65536];
        for (int i = 0; i < 65536; i++) {
            blockColors[i] = new Block(0, 0, true);
        }
        int i = 0;
        String line;
        while ((line = paramBufferedReader.readLine()) != null) {
            i++;
            if ((!line.trim().isEmpty()) && (!line.trim().startsWith("#"))) {
                String[] lineSplit = line.split("\t", 4);
                if (lineSplit.length < 2) {
                    System.err.println("Invalid color map line at " + paramString + ":" + i + ": " + line);
                } else {
                    int j = parseInt(lineSplit[1]);
                    if ("default".equals(lineSplit[0])) {
                        for (Block arrayOfBlock1 : blockColors) {
                            arrayOfBlock1.setBaseColor(j, 0, true);
                        }
                    } else {
                        String[] split = lineSplit[0].split(":", 2);
                        int m = parseInt(split[0]);
                        int n = split.length == 2 ? parseInt(split[1]) : -1;
                        int i1 = 0;
                        if (lineSplit.length > 2) {
                            switch (lineSplit[2]) {
                                case "biome_grass":
                                    i1 = 1;
                                    break;
                                case "biome_foliage":
                                    i1 = 2;
                                    break;
                                case "biome_water":
                                    i1 = 3;
                                    break;
                            }
                        }

                        if (n < 0) {
                            blockColors[(m & 0xFFFF)].setBaseColor(j, i1, false);
                        } else {
                            blockColors[(m & 0xFFFF)].setSubColor(n, j, i1);
                            blockColors[(m & 0xFFFF)].isDefault = false;
                        }
                    }
                }
            }
        }
        return new BlockMap(blockColors);
    }

    private static int parseInt(String paramString) {
        if (paramString.startsWith("0x")) {
            return (int) Long.parseLong(paramString.substring(2), 16);
        }
        return (int) Long.parseLong(paramString);
    }

    public static BlockMap load(File paramFile) throws IOException {
        try (BufferedReader localBufferedReader = new BufferedReader(new FileReader(paramFile))) {
            return load(localBufferedReader, paramFile.getPath());
        }
    }

    public static BlockMap loadDefault() {
        try {
            try (BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(BlockMap.class.getResourceAsStream("block-colors.txt")))) {
                return load(localBufferedReader, "(default block colors)");
            }
        } catch (IOException localIOException) {
            throw new RuntimeException("Error loading built-in color map", localIOException);
        }
    }

    public static class Block {

        protected static final int[] EMPTY_INT_ARRAY = new int[0];
        protected static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
        public static final int SUB_COLOR_COUNT = 16;
        public int baseColor;
        public int baseInfluence;
        public boolean isDefault;
        public int[] subColors = EMPTY_INT_ARRAY;
        public int[] subColorInfluences = EMPTY_INT_ARRAY;
        public boolean[] hasSubColors = EMPTY_BOOLEAN_ARRAY;

        private Block(int paramInt1, int paramInt2, boolean paramBoolean) {
            this.baseColor = paramInt1;
            this.baseInfluence = paramInt2;
            this.isDefault = paramBoolean;
        }

        private void setSubColor(int dataValue, int paramInt2, int paramInt3) {
            if ((dataValue < 0) || (dataValue >= 16)) {
                throw new RuntimeException("Block data value out of bounds: " + dataValue);
            }
            if (this.subColors.length == 0) {
                this.hasSubColors = new boolean[16];
                this.subColors = new int[16];
                this.subColorInfluences = new int[16];
            }
            this.hasSubColors[dataValue] = true;
            this.subColors[dataValue] = paramInt2;
            this.subColorInfluences[dataValue] = paramInt3;
        }

        private void setBaseColor(int paramInt1, int paramInt2, boolean paramBoolean) {
            this.baseColor = paramInt1;
            this.baseInfluence = paramInt2;
            this.isDefault = paramBoolean;
        }
    }

}
