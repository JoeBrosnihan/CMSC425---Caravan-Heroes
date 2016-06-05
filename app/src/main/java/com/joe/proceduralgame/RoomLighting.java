package com.joe.proceduralgame;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

public class RoomLighting {

	private final Room room;
	private List<StaticLightBody> lights = new ArrayList<>();

	private Bitmap lightMap;
	private int mapSize;
	private int mapTileSize;
	public int nMapColumns; //The number of columns (and rows) in the lightmap
	public float mapUVScale;
	public float mapUVOffset;

	public static abstract class StaticLightBody {
		/**
		 * Randomly samples this light, getting a random point within the light and the light's
		 * illumination at that point.
		 *
		 * @param result a float[6] that will store the {posX, posY, posZ, r, g, b} describing the sample
		 */
		public abstract void sample(float[] result);
	}

	public static class LightSphere extends StaticLightBody {
		private final float x, y, z, radius, r, g, b;

		public LightSphere(float x, float y, float z, float radius, float r, float g, float b) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public void sample(float[] result) {
			result[0] = x;
			result[1] = y;
			result[2] = z;
			result[3] = r;
			result[4] = g;
			result[5] = b;
		}
	}

	/**
	 * Creates a lighting system for a gen room
	 *
	 * @param room the room to be lit
	 */
	public RoomLighting(Room room) {
		this.room = room;
	}

	private final float[] sample = new float[6];
	private int computeIllumination(float[] position, float[] normal) {
		float r = 0;
		float g = 0;
		float b = 0;
		for (StaticLightBody light : lights) {
			light.sample(sample);

			float posToLightX = sample[0] - position[0];
			float posToLightY = sample[1] - position[1];
			float posToLightZ = sample[2] - position[2];

			float dist = (float) Math.sqrt(posToLightX * posToLightX + posToLightY * posToLightY + posToLightZ * posToLightZ);

			float ndot = (normal[0] * posToLightX + normal[1] * posToLightY + normal[2] * posToLightZ) / dist;
			ndot = Math.max(ndot, 0);
			float intensity = ndot / (dist * dist);

			r += sample[3] * intensity;
			g += sample[4] * intensity;
			b += sample[5] * intensity;
		}

		r = Math.min(Math.max(r, 0), 1);
		g = Math.min(Math.max(g, 0), 1);
		b = Math.min(Math.max(b, 0), 1);
		return Color.rgb((int) (255.0f * r), (int) (255.0f * g), (int) (255.0f * b));
	}

	private void computeLightmap() {
		final float[] normal = {0, 0, -1, 0};
		final float[] translatedNormal = new float[4];
		final float[] uvLocalPosition = new float[4];
		final float[] position = new float[4];

		final float divByTileSize = 1.0f / mapTileSize;

		for (int i = 0; i < room.staticQuads.size(); i++) {
			Quad quad = room.staticQuads.get(i);
			Matrix.multiplyMV(translatedNormal, 0, quad.modelMatrix, 0, normal, 0);

			final int minX = mapSize / nMapColumns * (i % nMapColumns);
			final int minY = mapSize / nMapColumns * (i / nMapColumns);
			for (int pixelU = 0; pixelU < mapTileSize; pixelU++) {
				for (int pixelV = 0; pixelV < mapTileSize; pixelV++) {
					uvLocalPosition[0] = pixelU * divByTileSize - .5f;
					uvLocalPosition[1] = .5f - pixelV * divByTileSize;
					uvLocalPosition[2] = 0;
					uvLocalPosition[3] = 1;
					Matrix.multiplyMV(position, 0, quad.modelMatrix, 0, uvLocalPosition, 0);

					int illuminationColor = computeIllumination(position, translatedNormal);
					lightMap.setPixel(minX + pixelU, minY + pixelV, illuminationColor);
				}
			}
		}
	}

	/**
	 * Adds a static light to this room's lighting.
	 *
	 * All lights should be added before the lighting is loaded.
	 *
	 * @param light the StaticLightBody light to add to the scene
	 */
	public void addStaticLight(StaticLightBody light) {
		lights.add(light);
	}

//	private void computeLightmap() {
//		for (int i = 0; i < room.staticQuads.size(); i++) {
//			final int minX = mapSize / nMapColumns * (i % nMapColumns);
//			final int minY = mapSize / nMapColumns * (i / nMapColumns);
//			for (int pixelX = minX; pixelX < minX + mapTileSize; pixelX++) {
//				for (int pixelY = minY; pixelY < minY + mapTileSize; pixelY++) {
//					lightMap.setPixel(pixelX, pixelY, Color.rgb(200, 200, 200));
//					lightMap.setPixel(minX, pixelY, Color.rgb(0, 0, 200));
//					lightMap.setPixel(minX + mapTileSize - 1, pixelY, Color.rgb(200, 200, 0));
//				}
//				lightMap.setPixel(pixelX, minY, Color.rgb(200, 0, 0));
//				lightMap.setPixel(pixelX, minY + mapTileSize - 1, Color.rgb(0, 200, 0));
//			}
//		}
//	}

	public void load(TextureManager tex) {
		mapSize = 256;
		lightMap = Bitmap.createBitmap(mapSize, mapSize, Bitmap.Config.RGB_565);

		nMapColumns = (int) Math.round(Math.pow(2, Math.ceil(Math.log10(room.staticQuads.size()) * Math.log(10) / Math.log(4))));
		mapTileSize = mapSize / nMapColumns;
		mapUVScale = (mapTileSize - 1) / (float) mapSize;
		mapUVOffset = .5f / mapSize;

		computeLightmap();

		tex.setLightmap(lightMap);
	}

}
