package com.joe.proceduralgame;

import android.graphics.Bitmap;
import android.graphics.Color;

public class RoomLighting {

	private final Room room;
	private Bitmap lightMap;
	private int mapSize;
	private int mapTileSize;
	public int nMapColumns; //The number of columns (and rows) in the lightmap
	public float mapUVScale;
	public float mapUVOffset;

	/**
	 * Creates a lighting system for a gen room
	 *
	 * @param room the room to be lit
	 */
	public RoomLighting(Room room) {
		this.room = room;
	}

	private void computeLightmap() {
		for (int i = 0; i < room.staticQuads.size(); i++) {
			final int minX = mapSize / nMapColumns * (i % nMapColumns);
			final int minY = mapSize / nMapColumns * (i / nMapColumns);
			for (int pixelX = minX; pixelX < minX + mapTileSize; pixelX++) {
				for (int pixelY = minY; pixelY < minY + mapTileSize; pixelY++) {
					lightMap.setPixel(pixelX, pixelY, Color.rgb(200, 200, 200));
					lightMap.setPixel(minX, pixelY, Color.rgb(0, 0, 200));
					lightMap.setPixel(minX + mapTileSize - 1, pixelY, Color.rgb(200, 200, 0));
				}
				lightMap.setPixel(pixelX, minY, Color.rgb(200, 0, 0));
				lightMap.setPixel(pixelX, minY + mapTileSize - 1, Color.rgb(0, 200, 0));
			}
		}
	}

	public void load(TextureManager tex) {
		mapSize = 128;
		lightMap = Bitmap.createBitmap(mapSize, mapSize, Bitmap.Config.RGB_565);

		nMapColumns = (int) Math.round(Math.pow(2, Math.ceil(Math.log10(room.staticQuads.size()) * Math.log(10) / Math.log(4))));
		mapTileSize = mapSize / nMapColumns;
		mapUVScale = (mapTileSize - 1) / (float) mapSize;
		mapUVOffset = .5f / mapSize;

		computeLightmap();

		tex.setLightmap(lightMap);
	}

}
