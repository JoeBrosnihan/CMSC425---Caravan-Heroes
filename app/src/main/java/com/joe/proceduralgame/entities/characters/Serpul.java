package com.joe.proceduralgame.entities.characters;

import com.joe.proceduralgame.Quad;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.R;
import com.joe.proceduralgame.TextureManager;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Serpul extends com.joe.proceduralgame.Character {

	private static final int N_COL = 8;
	private static final int GROUP = GROUP_ENEMY;
	private static final int ATTACK_HIT_TIME = 400, ATTACK_ANIMATION_TIME = 500,
			TAKING_DAMAGE_ANIMATION_TIME = 800, DEATH_ANIMATION_TIME = 1000;
	private static final int MAX_HITPOINTS = 2, MOVE_DISTANCE = 3;

	private int texture;
	private int atlasIndex = 0;

	public Serpul() {
		super(GROUP, ATTACK_HIT_TIME, ATTACK_ANIMATION_TIME, TAKING_DAMAGE_ANIMATION_TIME,
				DEATH_ANIMATION_TIME, MAX_HITPOINTS, MOVE_DISTANCE);
		setStrength(1);
		setDefense(0);
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
			animTime = (System.currentTimeMillis() - stateStartTime) % 1200;
			if (animTime < 600)
				newIndex = 39;
			else
				newIndex = 38;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 200)
				newIndex = 46;
			else if (animTime < 200)
				newIndex = 45;
			else
				newIndex = 44;
			break;
		case STATE_ATTACKING:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 100)
				newIndex = 46;
			else if (animTime < 200)
				newIndex = 55;
			else if (animTime < 400)
				newIndex = 54;
			else if (animTime < 500)
				newIndex = 53;
			else
				newIndex = 63;
			break;
		case STATE_TAKING_DAMAGE:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 400) {
				if (animTime % 160 >= 120)
					return;
				else
					newIndex = 47;
			} else {
				newIndex = 47;
			}
			break;
		case STATE_DEAD:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime % 160 >= 120)
				return;
			else
				newIndex = 62;
			break;
		}

		synchronized (this) {
			quad.uvOrigin[0] = (newIndex % N_COL) / (float) N_COL; // u coord
			quad.uvOrigin[1] = (newIndex / N_COL) / (float) N_COL; // v coord
			quad.uvScale[0] = scaleX / (float) N_COL; // u scale
			quad.uvScale[1] = scaleY / (float) N_COL; // v scale
			quad.draw(shaderProgram, mVPMatrix);
		}
	}

}
