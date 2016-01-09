package com.joe.proceduralgame;

import android.opengl.Matrix;

import java.util.LinkedList;

public abstract class Character extends AttackableEntity {
	
	public static final int STATE_WAITING = 0, STATE_WALKING = 1, STATE_ATTACKING = 2;
	public static final float TILT_ANGLE = 45f, VERTICAL_OFFSET = .25f, GROUND_LEVEL = .05f;
	
	protected float offsetX = 0, scaleX = 1, scaleY = 1;

	public final int attackHitTime;
	public final int attackAnimationTime;

	public float destx, destz;
	public int dir = 1;
	public float speed = 1.5f;
	public int state = 0;
	public long stateStartTime;
	public boolean stateActionPerformed;

	public Quad quad;

	//Action, target pair to be executed when the Character reaches its destination
	private Action.Pair queuedAction = null;
	private LinkedList<int[]> currentPath = null;
	private boolean playerOwned = false;
	//The entity that the Character is currently attacking
	//Set to the target every time attack(target) is called. TODO maybe set to null when done?
	private AttackableEntity attackTarget;

	/**
	 * Creates a new character with the given parameters
	 *
	 * @param attackHitTime the time of the frame on which this character's basic attack connects
	 *                      in ms
	 * @param attackAnimationTime the length of the basic attack animation in ms
	 */
	public Character(int attackHitTime, int attackAnimationTime) {
		super();
		this.attackHitTime = attackHitTime;
		this.attackAnimationTime = attackAnimationTime;
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

	public void attack(AttackableEntity target) {
		if (target.posx > posx)
			dir = 1;
		if (target.posx < posx)
			dir = -1;
		setState(STATE_ATTACKING);
		attackTarget = target;
	}

	public void walkPath(LinkedList<int[]> path) {
		currentPath = path;
		int[] dest = path.removeFirst();
		walkTo(currentRoom.originx + dest[1], currentRoom.originz + dest[0]);
	}

	/**
	 * Sets state to STATE_WALKING
	 */
	//TODO this should be made private once units only move via paths
	public void walkTo(float destx, float destz) {
		this.destx = destx;
		this.destz = destz;
		if (destx > posx)
			dir = 1;
		if (destx < posx)
			dir = -1;
		setState(STATE_WALKING);
	}

	/**
	 * Called when the character reaches the square it is walking to.
	 *
	 * This square may be just an intermediary square along a path it is walking or the final
	 * square itself.
	 */
	public void reachedDest() { // linear walk dest, not necessarily path dest.
		if (currentPath != null && !currentPath.isEmpty()) { //TODO at some point in development, currentPath should always be nonnull
			int[] dest = currentPath.removeFirst();
			walkTo(currentRoom.originx + dest[1], currentRoom.originz + dest[0]);
			return;
		}
		setState(STATE_WAITING);
		Action.Pair pair = dequeueAction();
		if (pair != null) {
			assert(pair.action.canPerform(this, pair.target));
			pair.action.perform(this, pair.target);
		}
	}

	/**
	 * Called to change the Character's state to a new state.
	 *
	 * If the state is new, resets stateActionPerformed and stateStartTime.
	 *
	 * @param newState the Character's new state
	 */
	public void setState(int newState) {
		if (state != newState) {
			stateActionPerformed = false;
			state = newState;
			stateStartTime = System.currentTimeMillis();
		}
	}

	public void destroy() {
		quad.destroy();
	}
	
	public boolean isPlayerOwned() {
		return playerOwned;
	}

	public boolean ownsQuad(Quad quad) {
		return quad == this.quad;
	}

	public void takeDamage(int damage) {
		//TODO take damage
		//TODO correctly set textureUnit to the UI texture with the numbers
		currentRoom.addDamageDisplay(new DamageDisplay(damage, quad.textureUnit, posx, posz));
	}

	public void setPlayerOwned(boolean val) {
		playerOwned = val;
	}

	/**
	 * Gets the target entity this character is currently attacking.
	 *
	 * Undefined behavior if not currently attacking.
	 * @return AttackableEntity target
	 */
	public AttackableEntity getAttackTarget() {
		return attackTarget;
	}

	/**
	 * Checks if a square is within this character's attack range
	 *
	 * @param targetRow
	 * @param targetCol
	 * @return true if the target square can be attacked
	 */
	public boolean isWithinAttackRange(int targetRow, int targetCol) {
		if (targetRow == gridRow)
			return targetCol == gridCol - 1 || targetCol == gridCol + 1;
		else if (targetCol == gridCol)
			return targetRow == gridRow - 1 || targetRow == gridRow + 1;
		return false;
	}

	/**
	 * Performs the Action on the target Entity when the Character reaches its destination
	 */
	public void enqueueAction(Action action, Entity target) {
		queuedAction = new Action.Pair(action, target);
	}

	/**
	 * Removes the queued Action pair
	 */
	public void clearAction() {
		queuedAction = null;
	}

	/**
	 * @return the queued Action.Pair or null
	 */
	private Action.Pair dequeueAction() {
		Action.Pair pair = queuedAction;
		queuedAction = null;
		return pair;
	}

}
