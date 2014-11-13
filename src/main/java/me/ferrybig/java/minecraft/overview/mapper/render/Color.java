package me.ferrybig.java.minecraft.overview.mapper.render;

public class Color {

    protected static final int clampByte(int paramInt) {
        if (paramInt < 0) {
            return 0;
        }
        if (paramInt > 255) {
            return 255;
        }
        return paramInt;
    }

    public static final int color(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
        return clampByte(paramInt1) << 24 | clampByte(paramInt2) << 16 | clampByte(paramInt3) << 8 | clampByte(paramInt4);
    }

    public static final int component(int paramInt1, int paramInt2) {
        return paramInt1 >> paramInt2 & 0xFF;
    }

    public static final int alpha(int paramInt) {
        return component(paramInt, 24);
    }

    public static final int shade(int paramInt1, int paramInt2) {
        return color(component(paramInt1, 24), component(paramInt1, 16) + paramInt2, component(paramInt1, 8) + paramInt2, component(paramInt1, 0) + paramInt2);
    }

    public static final int overlay(int paramInt1, int paramInt2) {
        int i = component(paramInt2, 24);
        int j = 255 - i;

        return color(i + component(paramInt1, 24) * j / 255, (component(paramInt2, 16) * i + component(paramInt1, 16) * j) / 255, (component(paramInt2, 8) * i + component(paramInt1, 8) * j) / 255, (component(paramInt2, 0) * i + component(paramInt1, 0) * j) / 255);
    }

    public static final int overlay(int paramInt1, int paramInt2, int paramInt3) {
        for (int i = 0; i < paramInt3; i++) {
            paramInt1 = overlay(paramInt1, paramInt2);
        }
        return paramInt1;
    }

    public static final int demultiplyAlpha(int paramInt) {
        int i = component(paramInt, 24);

        return i == 0 ? 0 : color(i, component(paramInt, 16) * 255 / i, component(paramInt, 8) * 255 / i, component(paramInt, 0) * 255 / i);
    }

    public static final int multiply(int paramInt1, int paramInt2) {
        return color(component(paramInt1, 24) * component(paramInt2, 24) / 255, component(paramInt1, 16) * component(paramInt2, 16) / 255, component(paramInt1, 8) * component(paramInt2, 8) / 255, component(paramInt1, 0) * component(paramInt2, 0) / 255);
    }

    public static final int multiplySolid(int paramInt1, int paramInt2) {
        return color(component(paramInt1, 24), component(paramInt1, 16) * component(paramInt2, 16) / 255, component(paramInt1, 8) * component(paramInt2, 8) / 255, component(paramInt1, 0) * component(paramInt2, 0) / 255);
    }
}
