package com.joe.proceduralgame.entities.characters;

import android.opengl.GLES20;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class SkeletonWarrior extends com.joe.proceduralgame.Character {
	
	private final int nCol = 8;

	private int texture;
	private int atlasIndex = 0;

	public SkeletonWarrior() {
		super(400, 1600);
	}

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		texture = tex.referenceLoad(R.drawable.atlas_demo2);
		quad = Quad.createDynamicQuad(Type.CHARACTER, new float[16], texture);
		updateModelMatrix();
	}

	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {
		int newIndex = 0;

		long animTime;
		switch (state) {
		case STATE_WAITING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 1600;
			if (animTime < 800)
				newIndex = 19;
			else
				newIndex = 20;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 150)
				newIndex = 21;
			else if (animTime < 300)
				newIndex = 22;
			else if (animTime < 450)
				newIndex = 23;
			else
				newIndex = 15;
			break;
		case STATE_ATTACKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 1000;
			if (animTime < 200)
				newIndex = 32;
			else if (animTime < 400)
				newIndex = 33;
			else if (animTime < 500)
				newIndex = 34;
			else
				newIndex = 36;
			break;
		}

		synchronized (this) {
			// as of right now, skele is always 1x1. No need to resize
			
			if (newIndex != atlasIndex) {
				atlasIndex = newIndex;
				switch (newIndex) {
				case 19:
				case 20: // idle
				case 21:
				case 22:
				case 23:
				case 15: // walking, standard
				case 32: // attack 1
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 33: // attack 2
					scaleX = 1;
					scaleY = 2;
					updateModelMatrix();
					break;
				case 34: // attack 3
					offsetX = .5f;
					scaleX = 2;
					scaleY = 2;
					updateModelMatrix();
					break;
				case 36: // attack 4
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
