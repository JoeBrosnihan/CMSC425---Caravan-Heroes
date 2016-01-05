package com.joe.proceduralgame;

import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;


public abstract class Entity {
	public float posx, posz;
	public int gridRow, gridCol;
	public Room currentRoom;
	public boolean graphicLoaded = false;
	
	public void setPosition(float x, float z) {
		posx = x;
		posz = z;
		if (currentRoom != null) {
			int oldRow = gridRow;
			int oldCol = gridCol;
			gridRow = Math.round(posz - currentRoom.originz);
			gridCol = Math.round(posx - currentRoom.originx);
			if (oldCol != gridCol || oldRow != gridRow) {
				currentRoom.moveEntity(this, oldRow, oldCol);
			}
		}
	}

	public void graphicLoad(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		synchronized (this) {
			load(tex);
			graphicLoaded = true;
		}
	}
	
	protected abstract void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion;
	public abstract void draw(int shaderProgram, float[] mVPMatrix);

}
