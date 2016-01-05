package com.joe.proceduralgame.entities.characters;

import android.opengl.GLES20;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Swordsman extends com.joe.proceduralgame.Character {
	
	private final int nCol = 8;

	private int texture;
	private int atlasIndex = 0;

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		texture = tex.referenceLoad(R.drawable.atlas_demo);
		quad = Quad.createDynamicQuad(Type.CHARACTER, new float[16], texture);
	}

	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {
		int newIndex = 0;

		long animTime;
		switch (state) {
		case STATE_WAITING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 1400;
			if (animTime < 700)
				newIndex = 0;
			else
				newIndex = 1;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 150)
				newIndex = 2;
			else if (animTime < 300)
				newIndex = 4;
			else if (animTime < 450)
				newIndex = 5;
			else
				newIndex = 6;
			break;
		}

		synchronized (this) {
			if (newIndex != atlasIndex) {
				atlasIndex = newIndex;
				switch (newIndex) {
				case 0:
				case 1: // idle
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 4:
				case 5:
				case 6: // walking, standard
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 2: // walking, odd frame
					offsetX = .5f;
					scaleX = 2;
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
