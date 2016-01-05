package com.joe.proceduralgame;

import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public final class TextureManager {

	public static final int MAX_COUNT = 30;
	public final int[][] unitToName = new int[MAX_COUNT][1];
	public Map<Integer, TextureEntry> idToUnit = new Hashtable<Integer, TextureEntry>();
	private Context context;
	
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
	
	private int loadTexture(int textureID) throws NoFreeTextureUnitsExcpetion {
		int unit_minus_1 = -1;
		for (int i = 0; i < MAX_COUNT; i++) {
			if (unitToName[i][0] == 0) {
				unit_minus_1 = i;
				break;
			}
		}
		if (unit_minus_1 == -1)
			throw new NoFreeTextureUnitsExcpetion();
		
//		TODO handle limited number of texture units.
//		TODO remove texture unit debugging code
//		int[] count = {-1};
//		GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, count, 0);
//		String n = this.context.getResources().getResourceName(textureID);
		GLES20.glGenTextures(1, unitToName[unit_minus_1], 0);
		DungeonRenderer.catchGLError();
		
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + unit_minus_1);
		DungeonRenderer.catchGLError();
	    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, unitToName[unit_minus_1][0]);
		DungeonRenderer.catchGLError();

	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		DungeonRenderer.catchGLError();
	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		DungeonRenderer.catchGLError();
//	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
//	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
		
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), textureID);
	    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		DungeonRenderer.catchGLError();
	    bitmap.recycle();
		
	    idToUnit.put(textureID, new TextureEntry(unit_minus_1 + 1, 1));
	    
		return unit_minus_1 + 1;
	}
	
	private void unloadTexture(int textureID) {
	}
	
	public int referenceLoad(int textureID) throws NoFreeTextureUnitsExcpetion {
		TextureEntry entry = idToUnit.get(textureID);
		if (entry != null) {
			entry.referenceCount++;
			return entry.glTextureUnit;
		} else {
			return loadTexture(textureID);
		}
	}
	
	public void referenceUnload(int textureID) {
		TextureEntry entry = idToUnit.get(textureID);
		entry.referenceCount--;
		if (entry.referenceCount == 0)
			unloadTexture(textureID);
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