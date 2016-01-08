package com.joe.proceduralgame.entities.characters;

import android.opengl.GLES20;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Swordsman extends com.joe.proceduralgame.Character {
	
	private final int nCol = 8;

	private int texture;
	private int atlasIndex = 0;

	public Swordsman() {
		super(250, 450);
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
			animTime = (System.currentTimeMillis() - stateStartTime) % 1400;
			if (animTime < 700)
				newIndex = 8;
			else
				newIndex = 9;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 150)
				newIndex = 0;
			else if (animTime < 300)
				newIndex = 1;
			else if (animTime < 450)
				newIndex = 3;
			else
				newIndex = 2;
			break;
		case STATE_ATTACKING:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 200)
				newIndex = 10;
			else if (animTime < 250)
				newIndex = 11;
			else if (animTime < 350)
				newIndex = 12;
			else
				newIndex = 17;
			break;
		}

		synchronized (this) {
			if (newIndex != atlasIndex) {
				atlasIndex = newIndex;
				switch (newIndex) {
				case 8:
				case 9: // idle
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 0:
				case 1:
				case 2: // walking, standard
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 3: // walking, odd frame
					offsetX = .5f;
					scaleX = 2;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 10:
				case 11: // attack, standard
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 12:
				case 17: //attack, arm extended
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
