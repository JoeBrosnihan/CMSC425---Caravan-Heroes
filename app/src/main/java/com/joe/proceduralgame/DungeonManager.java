package com.joe.proceduralgame;

import com.joe.proceduralgame.entities.characters.Ghoul;
import com.joe.proceduralgame.entities.characters.SkeletonWarrior;
import com.joe.proceduralgame.entities.characters.Swordsman;

import java.util.LinkedList;

public class DungeonManager extends Thread {

	private DungeonRenderer dungeonRenderer;
	private TextureManager textureManager;
	private Controller controller;
	
	boolean running = false;
	long waitMS = 10;
	
	Character leader;
	Room currentRoom;
	
	/**
	 * true if the player is in a room free of danger and can act freely.
	 */
	boolean neutral = false;
	private int phaseGroup = Character.GROUP_PLAYER;
	/** true if nothing is currently happening in the room, false if something is currently acting */
	private boolean tranquil = true;
	/** true if the phase will end as soon as the room becomes tranquil (tranquil == true). This
	 * value is set when all characters in the current phaseGroup have acted. */
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
						int damage = 666; //TODO calculate damage
						AttackableEntity target = c.getAttackTarget();
						target.takeHit(c, damage);
						c.stateActionPerformed = true;
						markCharacterActed(c);
						//Return the attack
						if (target instanceof Character) {
							//TODO handle a combat transaction better
							//Target returns the attack if the target didn't initiate it this turn
							Character targetCharacter = (Character) target;
							if (targetCharacter.getGroupID() != phaseGroup) {
								if (Action.basicAttack.canPerform(targetCharacter, c))
									targetCharacter.enqueueAction(Action.basicAttack, c);
							}
						}
					}
				} else {
					if (time - c.stateStartTime >= c.attackAnimationTime)
						c.finishAction();
				}
			}
		}

		if (!neutral && !tranquil && allWaiting)
			becomeTranquil();
	}

	/**
	 * Called to end the current phaseGroup
	 */
	public void endPhase() {
		waitingToEndPhase = false;
		for (Character c : currentRoom.characters) {
			c.actedThisTurn = false;
		}
		if (phaseGroup == Character.GROUP_PLAYER)
			beginPhase(Character.GROUP_ENEMY);
		else if (phaseGroup == Character.GROUP_ENEMY)
			beginPhase(Character.GROUP_PLAYER);
	}

	/**
	 * Begins the next group's phase
	 *
	 * @param newPhaseGroup the group id of the group of the next phase e.g. player, enemy
	 */
	private void beginPhase(int newPhaseGroup) {
		phaseGroup = newPhaseGroup;
		if (phaseGroup == Character.GROUP_PLAYER) {
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
	 * This method is called every time all units are waiting repeatedly until the enemy phaseGroup is
	 * over.
	 */
	private void makeEnemyMove() {
		Character actor = null;
		for (Character c : currentRoom.characters) {
			if (c.getGroupID() == Character.GROUP_ENEMY && !c.actedThisTurn) {
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
			assert actor.getGroupID() == phaseGroup;
			assert !actor.actedThisTurn;
		}
		tranquil = false;
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
			assert actor.getGroupID() == phaseGroup;
			assert !actor.actedThisTurn;
		}
		tranquil = false;
		dungeonRenderer.setFocus(actor);
		actor.clearAction();
		actor.walkPath(path);
		if (actor.isPlayerOwned())
			lastPlayerCommandedCharacter = actor;
	}

	/**
	 * Called when every action going on ends and every Character is in a waiting state.
	 *
	 * Triggers several events, including a the end of a turn if appropriate.
	 */
	private void becomeTranquil() {
		tranquil = true;

		if (waitingToEndPhase)
			endPhase();
		else if (phaseGroup == Character.GROUP_ENEMY)
			makeEnemyMove();

		controller.onBecomeTranquil();
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
			if (c.getGroupID() == phaseGroup) {
				if (!c.actedThisTurn) {
					readyToEnd = false;
					break;
				}
			}
		}
		if (readyToEnd)
			waitingToEndPhase = true;
	}

	/**
	 * Gets the group id of the current phase's character group.
	 *
	 * Characters with this group id may act during this phase.
	 */
	public int getPhaseGroup() {
		return phaseGroup;
	}

	/**
	 * Sets the dungeon renderer so this thread can send it information
	 *
	 * @param renderer the current dungeon renderer
	 */
	public void setDungeonRenderer(DungeonRenderer renderer) {
		this.dungeonRenderer = renderer;
	}

	/**
	 * Sets the controller so this thread can check its information
	 *
	 * @param controller the controller
	 */
	public void setController(Controller controller) {
		this.controller = controller;
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
