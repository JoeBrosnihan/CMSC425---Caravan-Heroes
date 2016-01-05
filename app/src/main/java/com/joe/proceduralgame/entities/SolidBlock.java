package com.joe.proceduralgame.entities;

import com.joe.proceduralgame.Entity;
import com.joe.proceduralgame.TextureManager;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class SolidBlock extends Entity {
	public static final SolidBlock singleton = new SolidBlock();

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {}
	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {}
}
