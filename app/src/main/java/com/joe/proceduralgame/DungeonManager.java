package com.joe.proceduralgame;

import com.joe.proceduralgame.entities.characters.Ghoul;
import com.joe.proceduralgame.entities.characters.SkeletonWarrior;
import com.joe.proceduralgame.entities.characters.Swordsman;

import java.util.LinkedList;

public class DungeonManager extends Thread {
	public static final int PLAYER_PHASE = 0, ENEMY_PHASE = 1;

	private DungeonRenderer dungeonRenderer;
	private TextureManager textureManager;
	
	boolean running = false;
	long waitMS = 10;
	
	Character leader;
	Room currentRoom;
	
	/***
	 * true if the player is in a room free of danger and can act freely.
	 */
	boolean neutral = true;
	int phase = PLAYER_PHASE;

	private boolean waitingToEndPhase = false;

	public DungeonManager(TextureManager manager) {
		this.textureManager = manager;
		this.setName("Dungeon Manager Thread");
		initialize();
	}
	
	/**
	 * Load the skeleton of the dungeon.
	 */
	public void initialize() {
		RoomGenerator gen = new RoomGenerator(17);
		currentRoom = new Room();
		currentRoom.generator = gen;
		gen.generate(currentRoom);
		
		leader = new Swordsman();
		currentRoom.addCharacter(leader);
		leader.setPlayerOwned(true);
		
		final Character enemy = new Ghoul();
		enemy.posx = 2;
		enemy.posz = 2;
		currentRoom.addCharacter(enemy);
		
		final Character enemy2 = new SkeletonWarrior();
		enemy2.posx = 3;
		enemy2.posz = 2;
		currentRoom.addCharacter(enemy2);
	}
	
	public void update(float dt) {
		long time = System.currentTimeMillis();
		for (Character c : currentRoom.characters) {
			if (c.state == Character.STATE_WALKING) {
				c.move(dt);
			} else if (c.state == Character.STATE_TAKING_DAMAGE) {
				if (time - c.stateStartTime >= c.takingDamageAnimationTime)
					c.finishAction();
			} else if (c.state == Character.STATE_ATTACKING) {
				if (!c.stateActionPerformed) {
					if (time - c.stateStartTime >= c.attackHitTime) {
						int damage = 27; //TODO calculate damage
						AttackableEntity target = c.getAttackTarget();
						target.takeHit(c, damage);
						c.stateActionPerformed = true;
						markCharacterActed(c);
						//Return the attack
						if (target instanceof Character) {
							//TODO handle a combat transaction better
							if (!((Character) target).isPlayerOwned()) {
								((Character) target).enqueueAction(Action.basicAttack, c);
							}
						}
					}
				} else {
					if (time - c.stateStartTime >= c.attackAnimationTime)
						c.finishAction();
				}
			}
		}
	}

	/**
	 * Commands the actor to walk a path then perform an action on a target.
	 *
	 * @param actor the Character that will perform the action
	 * @param path path to walk before performing the action
	 * @param action the action the Character will perform after the path has been walked
	 * @param target the target of the action
	 */
	public void commandAction(Character actor, LinkedList<int[]> path, Action action, Entity target) {
		if (!neutral) {
			if (actor.isPlayerOwned())
				assert (phase == PLAYER_PHASE);
			assert (!actor.actedThisTurn);
		}
		actor.enqueueAction(action, target);
		actor.walkPath(path);
	}

	/**
	 * Marks a character as having acted this turn.
	 *
	 * @param character the Character that acted
	 */
	private void markCharacterActed(Character character) {
		character.actedThisTurn = true;
	}

	/**
	 * Sets the dungeon renderer so this thread can send it information
	 *
	 * @param renderer the current dungeon renderer
	 */
	public void setDungeonRenderer(DungeonRenderer renderer) {
		this.dungeonRenderer = renderer;
	}

	public void run() {
		running = true;
		long last = System.currentTimeMillis();
		while (running) {
			long t = System.currentTimeMillis();
			long dt = t - last;
			if (dt < waitMS) {
				try {
					Thread.sleep(waitMS - dt);
				} catch (InterruptedException e) {}
			}
			last = System.currentTimeMillis();
			update(waitMS * .001f);
		}
	}

}
