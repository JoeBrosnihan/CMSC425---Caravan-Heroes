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
	
	/**
	 * true if the player is in a room free of danger and can act freely.
	 */
	boolean neutral = false;
	int phase = PLAYER_PHASE;

	private boolean waitingToEndPhase = false;
	/**
	 * The last commanded player owned Character
	 */
	private Character lastPlayerCommandedCharacter = null;

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
	
	public void update(float dt) { // TODO synchronize with touch events that affect the manager
		long time = System.currentTimeMillis();
		boolean allWaiting = true;
		for (Character c : currentRoom.characters) {
			if (c.state != Character.STATE_WAITING)
				allWaiting = false;

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

		if (!neutral && allWaiting) {
			if (waitingToEndPhase)
				endPhase();
			else if (phase == ENEMY_PHASE)
				makeEnemyMove();
		}
	}

	/**
	 * Called to end the current phase
	 */
	public void endPhase() {
		waitingToEndPhase = false;
		for (Character c : currentRoom.characters) {
			c.actedThisTurn = false;
		}
		if (phase == PLAYER_PHASE)
			beginPhase(ENEMY_PHASE);
		else if (phase == ENEMY_PHASE)
			beginPhase(PLAYER_PHASE);
	}

	/**
	 * Begins the next phase
	 *
	 * @param newPhase the phase type id e.g. player, enemy
	 */
	private void beginPhase(int newPhase) {
		phase = newPhase;
		if (phase == PLAYER_PHASE) {
			if (lastPlayerCommandedCharacter != null) {
				dungeonRenderer.setFocus(lastPlayerCommandedCharacter);
			} else {
				for (Character c : currentRoom.characters) {
					if (c.isPlayerOwned()) {
						dungeonRenderer.setFocus(c);
						break;
					}
				}
			}
		}
	}

	/**
	 * Issue an enemy command.
	 *
	 * This method is called every time all units are waiting repeatedly until the enemy phase is
	 * over.
	 */
	private void makeEnemyMove() {
		Character actor = null;
		for (Character c : currentRoom.characters) {
			if (!c.isPlayerOwned() && !c.actedThisTurn) {
				actor = c;
				break;
			}
		}
		if (actor == null)
			return;


		Character target = null;
		for (Character c : currentRoom.characters) {
			if (c.isPlayerOwned()) {
				target = c;
				break;
			}
		}
		if (target == null)
			return;

		LinkedList<int[]> path = currentRoom.findPath(actor.gridRow, actor.gridCol, target.gridRow,
				target.gridCol, false);
		if (path == null)
			return;

		commandAction(actor, path, Action.basicAttack, target);
	}

	/**
	 * Commands the actor to walk a path then perform an action on a target.
	 *
	 * @param actor the Character that will perform the action
	 * @param path LinkedList of length 2 int[]s {row, col} corresponding to squares to walk along
	 * @param action the action the Character will perform after the path has been walked
	 * @param target the target of the action
	 */
	public void commandAction(Character actor, LinkedList<int[]> path, Action action, Entity target) {
		if (!neutral) {
			if (actor.isPlayerOwned())
				assert (phase == PLAYER_PHASE);
			assert (!actor.actedThisTurn);
		}
		dungeonRenderer.setFocus(actor);
		actor.enqueueAction(action, target);
		actor.walkPath(path);
		if (actor.isPlayerOwned())
			lastPlayerCommandedCharacter = actor;
	}

	/**
	 * Commands the actor to walk a path
	 *
	 * @param actor the Character that will walk the path
	 * @param path LinkedList of length 2 int[]s {row, col} corresponding to squares to walk along
	 */
	public void commandMove(Character actor, LinkedList<int[]> path) {
		if (!neutral) {
			if (actor.isPlayerOwned())
				assert (phase == PLAYER_PHASE);
			assert (!actor.actedThisTurn);
		}
		dungeonRenderer.setFocus(actor);
		actor.clearAction();
		actor.walkPath(path);
		if (actor.isPlayerOwned())
			lastPlayerCommandedCharacter = actor;
	}

	/**
	 * Marks a character as having acted this turn if not in neutral.
	 *
	 * @param character the Character that acted
	 */
	private void markCharacterActed(Character character) {
		if (neutral)
			return;
		character.actedThisTurn = true;

		boolean readyToEnd = true;
		for (Character c : currentRoom.characters) {
			if (phase == PLAYER_PHASE) {
				if (c.isPlayerOwned() && !c.actedThisTurn) {
					readyToEnd = false;
					break;
				}
			} else if (phase == ENEMY_PHASE) {
				if (!c.isPlayerOwned() && !c.actedThisTurn) {
					readyToEnd = false;
					break;
				}
			}
		}
		if (readyToEnd)
			waitingToEndPhase = true;
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
