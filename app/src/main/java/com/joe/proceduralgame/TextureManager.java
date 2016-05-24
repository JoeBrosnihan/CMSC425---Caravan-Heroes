package com.joe.proceduralgame;

import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public final class TextureManager {

	private static final int MIN_UNIT = 2; //Units less than this are reserved
	public static final int LIGHTMAP_UNIT = 1;
	public static final int MAX_COUNT = 30;

	public final int[][] unitToName = new int[MAX_COUNT][1];
	public Map<Integer, TextureEntry> idToUnit = new Hashtable<Integer, TextureEntry>();
	public Context context;
	
	public static class NoFreeTextureUnitsExcpetion extends Exception {}
	
	private static class TextureEntry {
		int glTextureUnit;
		int referenceCount = 0;
		public TextureEntry(int unit, int refCount) {
			this.glTextureUnit = unit;
			this.referenceCount = refCount;
		}
	}
	
	public TextureManager(Context context) {
		this.context = context;
	}

	/**
	 * Binds a bitmap to a texture unit
	 *
	 * @param targetUnit the texture unit to bind the bitmap to
	 * @param bitmap the Bitmap to load with OpenGL
	 */
	private void setBitmap(int targetUnit, Bitmap bitmap){
		if (unitToName[targetUnit][0] != 0)
			GLES20.glDeleteTextures(1, unitToName[targetUnit], 0);

		GLES20.glGenTextures(1, unitToName[targetUnit], 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + targetUnit);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, unitToName[targetUnit][0]);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		DungeonRenderer.catchGLError();
	}

	/**
	 * Finds a texture unit not in use and binds the provided bitmap to it.
	 *
	 * @param bitmap the bitmap to load with OpenGL
	 * @return the texture unit assigned to the bitmap
	 * @throws NoFreeTextureUnitsExcpetion throw when there are no free texture units left
	 */
	private int loadBitmap(Bitmap bitmap) throws NoFreeTextureUnitsExcpetion {
		int unit = -1;
		for (int i = MIN_UNIT; i < MAX_COUNT; i++) {
			if (unitToName[i][0] == 0) {
				unit = i;
				break;
			}
		}
		if (unit == -1)
			throw new NoFreeTextureUnitsExcpetion();
		setBitmap(unit, bitmap);

		return unit;
	}

	/**
	 * Finds a texture unit not in use and binds bitmap of the provided resource to it.
	 *
	 * @param textureID the resource id of the image to load
	 * @return the texture unit assigned to the bitmap
	 * @throws NoFreeTextureUnitsExcpetion throw when there are no free texture units left
	 */
	private int loadTextureResource(int textureID) throws NoFreeTextureUnitsExcpetion {
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), textureID);
		int unit = loadBitmap(bitmap);
	    bitmap.recycle();

	    idToUnit.put(textureID, new TextureEntry(unit, 1));
		return unit;
	}
	
	private void unloadTextureResource(int textureID) {
	}

	public int referenceLoad(int textureID) throws NoFreeTextureUnitsExcpetion {
		TextureEntry entry = idToUnit.get(textureID);
		if (entry != null) {
			entry.referenceCount++;
			return entry.glTextureUnit;
		} else {
			return loadTextureResource(textureID);
		}
	}
	
	public void referenceUnload(int textureID) {
		TextureEntry entry = idToUnit.get(textureID);
		entry.referenceCount--;
		if (entry.referenceCount == 0)
			unloadTextureResource(textureID);
	}

	/**
	 * Loads a lightmap Bitmap with OpenGL
	 * The bound texture unit will be LIGHTMAP_UNIT
	 *
	 * @param lightmap the Bitmap to load as the lightmap
	 */
	public void setLightmap(Bitmap lightmap) {
		setBitmap(LIGHTMAP_UNIT, lightmap);
	}
	
	/**
	 * utility, given an index on a width x width tiled atlas, get the U coordinate of the origin of the texture at that index
	 * @param index
	 * @param width
	 * @return
	 */
	public static float atlasU(int index, int width) {
		return (index % width) / (float) width;
	}
	
	/**
	 * utility, given an index on a width x width tiled atlas, get the V coordinate of the origin of the texture at that index
	 * @param index
	 * @param width
	 * @return
	 */
	public static float atlasV(int index, int width) {
		return (index / width) / (float) width;
	}
	
}