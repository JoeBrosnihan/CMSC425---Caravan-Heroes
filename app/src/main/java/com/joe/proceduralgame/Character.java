package com.joe.proceduralgame;

import android.media.MediaPlayer;
import android.opengl.Matrix;

import java.util.LinkedList;

/**
 * Represents an AttackableEntity that can move and possibly attack.
 */
public abstract class Character extends AttackableEntity {
	public static final int GROUP_PLAYER = 0, GROUP_ENEMY = 1;
	public static final int STATE_WAITING = 0, STATE_WALKING = 1, STATE_ATTACKING = 2,
			STATE_TAKING_DAMAGE = 3;
	public static final float TILT_ANGLE = 45f, VERTICAL_OFFSET = .25f, GROUND_LEVEL = .05f;
	
	protected float offsetX = 0, scaleX = 1, scaleY = 1;

	public final int attackHitTime;
	public final int attackAnimationTime;
	public final int takingDamageAnimationTime;

	public float destx, destz;
	public int dir = 1;
	public float speed = 1.5f;
	public int state = 0;
	public long stateStartTime;
	/**
	 * Set to false every time state is changed.
	 * Set to true to signify that the event associated with the state has been fired and should
	 * not be fired again.
	 */
	public boolean stateActionPerformed;
	public boolean actedThisTurn = false;
	public MediaPlayer attackSound;

	public Quad quad;

	//Action, target pair to be executed when the Character reaches its destination
	private Action.Pair queuedAction = null;
	private LinkedList<int[]> currentPath = null;
	//The entity that the Character is currently attacking
	//Set to the target every time attack(target) is called. TODO maybe set to null when done?
	private AttackableEntity attackTarget;
	//An integer representing the group, or team, this unit is on
	private int groupID;

	/**
	 * Creates a new character with the given parameters
	 *
	 * @param groupID the id of the group this character falls into
	 * @param attackHitTime the time of the frame on which this character's basic attack connects
	 *                      in ms
	 * @param attackAnimationTime the length of the basic attack animation in ms
	 * @param takingDamageAnimationTime the length of the taking damage animation in ms
	 */
	public Character(int groupID, int attackHitTime, int attackAnimationTime, int takingDamageAnimationTime) {
		super();
		this.groupID = groupID;
		this.attackHitTime = attackHitTime;
		this.attackAnimationTime = attackAnimationTime;
		this.takingDamageAnimationTime = takingDamageAnimationTime;
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
		if (attackSound != null)
			attackSound.start();
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
		} else {
			finishAction();
		}
	}

	/**
	 * Called when the character is finished an action and wants to return to a waiting state.
	 * Performs a queued action if there is one.
	 */
	public void finishAction() {
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
	private void setState(int newState) {
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
		return groupID == GROUP_PLAYER;
	}

	@Override
	public boolean ownsQuad(Quad quad) {
		return quad == this.quad;
	}

	@Override
	public void takeHit(Character attacker, int damage) {
		//TODO take damage
		if (attacker.posx < posx)
			dir = -1;
		if (attacker.posx > posx)
			dir = 1;
		setState(STATE_TAKING_DAMAGE);
		currentRoom.addDamageDisplay(new DamageDisplay(damage, quad.textureUnit, posx, posz));
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
	 * Gets the id of the group this character is in
	 */
	public int getGroupID() {
		return groupID;
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
