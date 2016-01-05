package com.joe.proceduralgame;

import android.opengl.Matrix;

public abstract class Character extends Entity {
	
	public static final int STATE_WAITING = 0, STATE_WALKING = 1;
	public static final float TILT_ANGLE = 45f, VERTICAL_OFFSET = .25f, GROUND_LEVEL = .05f;
	
	protected float offsetX = 0, scaleX = 1, scaleY = 1;
	
	public float destx, destz;
	public int dir = 1;
	public float speed = 1.5f;
	public int state = 0;
	public long stateStartTime;
	
	public Quad quad;
	
	public Character() {
		super();
	}
	
	@Override
	public void setPosition(float x, float z) {
		super.setPosition(x, z);
		synchronized (this) {
			if (graphicLoaded)
				updateModelMatrix();
		}
	}
	
	public void updateModelMatrix() {
		Matrix.setIdentityM(quad.modelMatrix, 0);
		Matrix.translateM(quad.modelMatrix, 0, posx, 0, posz + VERTICAL_OFFSET);
		Matrix.rotateM(quad.modelMatrix, 0, TILT_ANGLE, -1, 0, 0);
		Matrix.translateM(quad.modelMatrix, 0, dir * offsetX, scaleY * .5f - GROUND_LEVEL, 0);
		Matrix.scaleM(quad.modelMatrix, 0, dir * scaleX, scaleY, 1);
	}
	
	public void move(float dt) {
		float dx = destx - posx;
		float dy = destz - posz;
		float d = (float) Math.hypot(dx, dy);
		if (d <= speed * dt) {
			setPosition(destx, destz);
			reachedDest();
		} else {
			setPosition(posx + dx / d * speed * dt, posz += dy / d * speed * dt);
		}
	}
	
	public void walkTo(float destx, float destz) {
		this.destx = destx;
		this.destz = destz;
		if (destx > posx)
			dir = 1;
		if (destx < posx)
			dir = -1;
		setState(STATE_WALKING);
	}
	
	public void reachedDest() { // linear walk dest, not necessarily path dest.
		setState(STATE_WAITING);
	}
	
	public void setState(int newState) {
		if (state != newState) {
			state = newState;
			stateStartTime = System.currentTimeMillis();
		}
	}

	public void destroy() {
		quad.destroy();
	}
	
	public boolean isPlayerSelectable() {
		return true;
	}
	
}
