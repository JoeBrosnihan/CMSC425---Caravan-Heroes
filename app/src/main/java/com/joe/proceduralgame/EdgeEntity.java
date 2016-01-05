package com.joe.proceduralgame;

import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public abstract class EdgeEntity {
	public static final int DIR_VERTICAL = 0, DIR_HORIZONTAL = 1;
	
	public float posx, posz;
	public int dir;
	public int edgesIndex;
	public Room currentRoom;

	public abstract void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion;
	public abstract void draw(int shaderProgram, float[] mVPMatrix);

}
