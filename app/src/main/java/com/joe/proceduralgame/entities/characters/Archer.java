package com.joe.proceduralgame.entities.characters;

import android.media.MediaPlayer;

import com.joe.proceduralgame.Quad;
import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.R;
import com.joe.proceduralgame.TextureManager;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Archer extends com.joe.proceduralgame.Character {

	private static final int N_COL = 8;
	private static final int GROUP = GROUP_PLAYER;
	private static final int ATTACK_HIT_TIME = 500, ATTACK_ANIMATION_TIME = 600,
			TAKING_DAMAGE_ANIMATION_TIME = 800, DEATH_ANIMATION_TIME = 1000;
	private static final int MAX_HITPOINTS = 5, MOVE_DISTANCE = 5;

	private int texture;
	private int atlasIndex = 0;

	public Archer() {
		super(GROUP, ATTACK_HIT_TIME, ATTACK_ANIMATION_TIME, TAKING_DAMAGE_ANIMATION_TIME,
				DEATH_ANIMATION_TIME, MAX_HITPOINTS, MOVE_DISTANCE);
		setStrength(4);
		setDefense(1);
	}

	@Override
	public int getIconID() {
		return R.drawable.archer_icon;
	}

	@Override
	protected void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		texture = tex.referenceLoad(R.drawable.atlas_demo2);
		quad = Quad.createDynamicQuad(Type.CHARACTER, new float[16], texture);
		attackSound = MediaPlayer.create(tex.context, R.raw.bow_and_arrow_attack);
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
				newIndex = 40;
			else
				newIndex = 48;
			break;
		case STATE_WALKING:
			animTime = (System.currentTimeMillis() - stateStartTime) % 600;
			if (animTime < 150)
				newIndex = 49;
			else if (animTime < 300)
				newIndex = 50;
			else if (animTime < 450)
				newIndex = 51;
			else
				newIndex = 52;
			break;
		case STATE_ATTACKING:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 100)
				newIndex = 56;
			else if (animTime < 300)
				newIndex = 57;
			else if (animTime < 500)
				newIndex = 58;
			else if (animTime < 550)
				newIndex = 59;
			else
				newIndex = 60;
			break;
		case STATE_TAKING_DAMAGE:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime < 400) {
				if (animTime % 160 >= 120)
					return;
				else
					newIndex = 40;
			} else {
				newIndex = 40;
			}
			break;
		case STATE_DEAD:
			animTime = System.currentTimeMillis() - stateStartTime;
			if (animTime % 160 >= 120)
				return;
			else
				newIndex = 48;
			break;
		}

		synchronized (this) {
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
