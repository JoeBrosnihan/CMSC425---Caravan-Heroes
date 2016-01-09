package com.joe.proceduralgame.entities.characters;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Ghoul extends com.joe.proceduralgame.Character {
	
	private static final int N_COL = 8, ATTACK_HIT_TIME = 400, ATTACK_ANIMATION_TIME = 1600,
			TAKING_DAMAGE_ANIMATION_TIME = 800;

	private int texture;
	private int atlasIndex = 0;

	public Ghoul() {
		super(ATTACK_HIT_TIME, ATTACK_ANIMATION_TIME, TAKING_DAMAGE_ANIMATION_TIME);
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
		case STATE_TAKING_DAMAGE:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 400) {
				if (animTime % 160 >= 120)
					return;
				else
					newIndex = 24;
			} else {
				newIndex = 24;
			}
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
			
			quad.uvOrigin[0] = (newIndex % N_COL) / (float) N_COL; // u coord
			quad.uvOrigin[1] = (newIndex / N_COL) / (float) N_COL; // v coord
			quad.uvScale[0] = scaleX / (float) N_COL; // u scale
			quad.uvScale[1] = scaleY / (float) N_COL; // v scale
			quad.draw(shaderProgram, mVPMatrix);
		}
	}

}
