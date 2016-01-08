package com.joe.proceduralgame.entities.characters;

import android.opengl.GLES20;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Ghoul extends com.joe.proceduralgame.Character {
	
	private final int nCol = 8;

	private int texture;
	private int atlasIndex = 0;

	public Ghoul() {
		super(300, 500);
	}

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		texture = tex.referenceLoad(R.drawable.atlas_demo2);
		quad = Quad.createDynamicQuad(Type.CHARACTER, new float[16], texture);
	}

	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {
		int newIndex = 0;

		long animTime;
		switch (state) {
		case STATE_WAITING:
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 400;
			if (animTime < 200)
				newIndex = 24;
			else
				newIndex = 25;
			break;
		}

		synchronized (this) {
			if (newIndex != atlasIndex) {
				atlasIndex = newIndex;
				switch (newIndex) {
				case 24:
				case 25: // waiting/walking, standard
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				}
			}
			
			quad.uvOrigin[0] = (newIndex % nCol) / (float) nCol; // u coord
			quad.uvOrigin[1] = (newIndex / nCol) / (float) nCol; // v coord
			quad.uvScale[0] = scaleX / (float) nCol; // u scale
			quad.uvScale[1] = scaleY / (float) nCol; // v scale
			quad.draw(shaderProgram, mVPMatrix);
		}
	}

}
