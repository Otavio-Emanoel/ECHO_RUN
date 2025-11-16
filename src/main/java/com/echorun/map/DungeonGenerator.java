package com.echorun.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    public static class Room {
        public int x, y, w, h;
        public Point center() { return new Point(x + w / 2, y + h / 2); }
    }

    private final int width;
    private final int height;
    private final int tileSize;
    private final long seed;

    private final Random rng;

    public DungeonGenerator(int width, int height, int tileSize, long seed) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public DungeonMap generate() {
        Tile[][] tiles = new Tile[height][width];
        // Fill walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = Tile.WALL;
            }
        }

        int attempts = 80;
        int minSize = 6;
        int maxSize = 14;
        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            int w = minSize + rng.nextInt(maxSize - minSize + 1);
            int h = minSize + rng.nextInt(maxSize - minSize + 1);
            int x = 1 + rng.nextInt(Math.max(1, width - w - 2));
            int y = 1 + rng.nextInt(Math.max(1, height - h - 2));
            Room r = new Room();
            r.x = x; r.y = y; r.w = w; r.h = h;
            if (overlaps(r, rooms)) continue;
            rooms.add(r);
            carveRoom(tiles, r);
        }

        // Conectar salas em uma spanning tree simples
        rooms.sort((a, b) -> Integer.compare(a.x, b.x));
        for (int i = 1; i < rooms.size(); i++) {
            Room a = rooms.get(i - 1);
            Room b = rooms.get(i);
            connect(tiles, a.center(), b.center());
        }

        // Portas simples: marcar junções parede->chão
        placeDoors(tiles);

        return new DungeonMap(width, height, tileSize, tiles, seed);
    }

    private boolean overlaps(Room r, List<Room> rooms) {
        Rectangle R = new Rectangle(r.x - 1, r.y - 1, r.w + 2, r.h + 2);
        for (Room o : rooms) {
            Rectangle O = new Rectangle(o.x, o.y, o.w, o.h);
            if (R.intersects(O)) return true;
        }
        return false;
    }

    private void carveRoom(Tile[][] tiles, Room r) {
        for (int y = r.y; y < r.y + r.h; y++) {
            for (int x = r.x; x < r.x + r.w; x++) {
                tiles[y][x] = Tile.FLOOR;
            }
        }
    }

    private void connect(Tile[][] tiles, Point a, Point b) {
        // L-carve: horizontal then vertical (ou vice-versa)
        if (rng.nextBoolean()) {
            carveH(tiles, a.x, b.x, a.y);
            carveV(tiles, a.y, b.y, b.x);
        } else {
            carveV(tiles, a.y, b.y, a.x);
            carveH(tiles, a.x, b.x, b.y);
        }
    }

    private void carveH(Tile[][] tiles, int x1, int x2, int y) {
        int from = Math.min(x1, x2); int to = Math.max(x1, x2);
        for (int x = from; x <= to; x++) tiles[y][x] = Tile.FLOOR;
    }

    private void carveV(Tile[][] tiles, int y1, int y2, int x) {
        int from = Math.min(y1, y2); int to = Math.max(y1, y2);
        for (int y = from; y <= to; y++) tiles[y][x] = Tile.FLOOR;
    }

    private void placeDoors(Tile[][] tiles) {
        int h = tiles.length; int w = tiles[0].length;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (tiles[y][x] != Tile.WALL) continue;
                int floors = 0;
                if (tiles[y][x-1] == Tile.FLOOR) floors++;
                if (tiles[y][x+1] == Tile.FLOOR) floors++;
                if (tiles[y-1][x] == Tile.FLOOR) floors++;
                if (tiles[y+1][x] == Tile.FLOOR) floors++;
                if (floors >= 2) tiles[y][x] = Tile.DOOR;
            }
        }
    }
}
