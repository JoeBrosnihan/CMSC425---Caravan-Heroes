package com.joe.proceduralgame.entities;

import android.opengl.Matrix;

import com.joe.proceduralgame.DungeonRenderer;
import com.joe.proceduralgame.EdgeEntity;
import com.joe.proceduralgame.Quad;
import com.joe.proceduralgame.R;
import com.joe.proceduralgame.TextureManager;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Door extends EdgeEntity {
	public int dir;
	public Quad doorQuad, frameQuad;
	public float openAngle = 30f;
	
	@Override
	public void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion { // TODO load all entities in a room
		float[] frameModel = new float[16];
		Matrix.setIdentityM(frameModel, 0);
		Matrix.translateM(frameModel, 0, posx, .55f, posz);
		Matrix.scaleM(frameModel, 0, 1f, 1.1f, 1f);
		Matrix.rotateM(frameModel, 0, 90f * (dir - 1), 0, 1, 0);
		frameQuad = Quad.createDynamicQuad(Quad.Type.DOOR, frameModel, tex.referenceLoad(R.drawable.doorframe1));
		DungeonRenderer.catchGLError();
		doorQuad = Quad.createDynamicQuad(Quad.Type.DOOR, new float[16], tex.referenceLoad(R.drawable.door1));
		DungeonRenderer.catchGLError();
	}

	@Override
	public void draw(int shaderProgram, float[] mVPMatrix) {
		openAngle = (float) Math.sin(System.currentTimeMillis() * .001) * 45f;
		
		float doorShift = .25f;
		Matrix.translateM(doorQuad.modelMatrix, 0, frameQuad.modelMatrix, 0, -doorShift, 0, 0);
		Matrix.rotateM(doorQuad.modelMatrix, 0, openAngle, 0, 1, 0);
		Matrix.translateM(doorQuad.modelMatrix, 0, doorShift, 0, 0);
		
		frameQuad.draw(shaderProgram, mVPMatrix);
		doorQuad.draw(shaderProgram, mVPMatrix);
	}

}
