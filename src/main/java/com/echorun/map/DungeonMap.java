package com.echorun.map;

import java.util.Random;

public class DungeonMap {
    private final int width;
    private final int height;
    private final int tileSize;
    private final Tile[][] tiles;
    private final long seed;

    public DungeonMap(int width, int height, int tileSize, Tile[][] tiles, long seed) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tiles = tiles;
        this.seed = seed;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTileSize() { return tileSize; }
    public Tile get(int x, int y) { return tiles[y][x]; }
    public long getSeed() { return seed; }

    public boolean isWalkable(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return false;
        Tile t = get(tx, ty);
        return t == Tile.FLOOR || t == Tile.DOOR;
    }

    public int clampX(int x) { return Math.max(0, Math.min(width - 1, x)); }
    public int clampY(int y) { return Math.max(0, Math.min(height - 1, y)); }

    // Utilitário simples para pequenas variações pseudo-aleatórias
    public float random01(int x, int y) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        // Converte para [0,1)
        return (h >>> 11) / (float)(1L << 53);
    }
}
