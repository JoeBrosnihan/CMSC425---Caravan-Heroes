package com.joe.proceduralgame;

import android.opengl.GLES20;

import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class SkeletonWarrior extends Character {
	
	private final int nCol = 8;

	private int texture;
	private int atlasIndex = 0;

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		texture = tex.referenceLoad(R.drawable.atlas_demo);
		quad = Quad.createDynamicQuad(Type.CHARACTER, new float[16], texture);
		updateModelMatrix();
	}

	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {
		int newIndex = 0;

		long animTime;
		switch (state) {
		case STATE_WAITING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 1400;
			if (animTime < 700)
				newIndex = 11;
			else
				newIndex = 12;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 150)
				newIndex = 13;
			else if (animTime < 300)
				newIndex = 14;
			else if (animTime < 450)
				newIndex = 15;
			else
				newIndex = 16;
			break;
		}

		synchronized (this) {
			// as of right now, skele is always 1x1. No need to resize
			
//			if (newIndex != atlasIndex) {
//				atlasIndex = newIndex;
//				switch (newIndex) {
//				case 11:
//				case 12: // idle
//				case 13:
//				case 14:
//				case 16:
//				case 15: // walking, standard
//					offsetX = 0;
//					scaleX = 1;
//					scaleY = 1;
//					updateModelMatrix();
//					break;
//				}
//			}
			
			quad.uvOrigin[0] = (newIndex % nCol) / (float) nCol; // u coord
			quad.uvOrigin[1] = (newIndex / nCol) / (float) nCol; // v coord
			quad.uvScale[0] = scaleX / (float) nCol; // u scale
			quad.uvScale[1] = scaleY / (float) nCol; // v scale
			quad.draw(shaderProgram, mVPMatrix);
		}
	}

}
