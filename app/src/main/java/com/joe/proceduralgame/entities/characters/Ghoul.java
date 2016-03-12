package com.joe.proceduralgame.entities.characters;

import com.joe.proceduralgame.*;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Ghoul extends com.joe.proceduralgame.Character {
	
	private static final int N_COL = 8;
	private static final int GROUP = GROUP_ENEMY;
	private static final int ATTACK_HIT_TIME = 400, ATTACK_ANIMATION_TIME = 500,
			TAKING_DAMAGE_ANIMATION_TIME = 800, DEATH_ANIMATION_TIME = 1000;
	private static final int MAX_HITPOINTS = 4, MOVE_DISTANCE = 5;

	private int texture;
	private int atlasIndex = 0;

	public Ghoul() {
		super(GROUP, ATTACK_HIT_TIME, ATTACK_ANIMATION_TIME, TAKING_DAMAGE_ANIMATION_TIME,
				DEATH_ANIMATION_TIME, MAX_HITPOINTS, MOVE_DISTANCE);
		setStrength(3);
		setDefense(1);
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
		case STATE_ATTACKING:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 200)
				newIndex = 26;
			else if (animTime < 400)
				newIndex = 28;
			else
				newIndex = 30;
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
		case STATE_DEAD:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime % 160 >= 120)
				return;
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
				case 26: // attack, 1x1
					offsetX = 0;
					scaleX = 1;
					scaleY = 1;
					updateModelMatrix();
					break;
				case 28:
				case 30: // attack, outstretched
					offsetX = .5f;
					scaleX = 2;
					scaleY = 1;
					updateModelMatrix();
					break;
				}
			}
			
			quad.uvOrigin[0] = (newIndex % N_COL) / (float) N_COL; // u coord
			quad.uvOrigin[1] = (newIndex / N_COL) / (float) N_COL; // v coord
			quad.uvScale[0] = scaleX / (float) N_COL; // u scale
			quad.uvScale[1] = scaleY / (float) N_COL; // v scale

			preDraw(shaderProgram, mVPMatrix);
			quad.draw(shaderProgram, mVPMatrix);
			postDraw(shaderProgram, mVPMatrix);
		}
	}

}
