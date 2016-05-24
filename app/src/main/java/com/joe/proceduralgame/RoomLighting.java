package com.joe.proceduralgame;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

public class RoomLighting {

	private final Room room;
	private Bitmap lightMap;
	private int mapSize;
	public int nMapColumns; //The number of columns (and rows) in the lightmap
	public float mapColumnWidth;

	/**
	 * Creates a lighting system for a gen room
	 *
	 * @param room the room to be lit
	 */
	public RoomLighting(Room room) {
		this.room = room;
	}

	public void load(TextureManager tex) {
		mapSize = 128;
		lightMap = Bitmap.createBitmap(mapSize, mapSize, Bitmap.Config.RGB_565);

		nMapColumns = (int) Math.round(Math.ceil(Math.sqrt(room.staticQuads.size())));
		mapColumnWidth = 1 / (float) nMapColumns;

		lightMap.eraseColor(Color.BLACK);

		for (int r = 0; r < mapSize; r++) {
			for (int c = r; c < mapSize; c++) {
				lightMap.setPixel(r, c, Color.rgb(200, 180, 150));
			}
		}

		tex.setLightmap(lightMap);
	}

}
