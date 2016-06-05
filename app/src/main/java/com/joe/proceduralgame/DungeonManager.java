package com.joe.proceduralgame;

import com.joe.proceduralgame.entities.characters.Archer;
import com.joe.proceduralgame.entities.characters.Ghoul;
import com.joe.proceduralgame.entities.characters.Serpul;
import com.joe.proceduralgame.entities.characters.SkeletonWarrior;
import com.joe.proceduralgame.entities.characters.Swordsman;

import java.util.Iterator;
import java.util.LinkedList;

public class DungeonManager extends Thread {

	public static final int PHASE_TRANSITION_TIME = 1500;

	private DungeonRenderer dungeonRenderer;
	private Controller controller;
	private GUIManager guiManager;
	
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
	/** If true, show an overlay and don't update anything for an interval. */
	private boolean transitioningPhase = false;
	/** The time in ms of when the phase transition started */
	private long transitionStartTime;
	/**
	 * The last commanded player owned Character
	 */
	private Character lastPlayerCommandedCharacter = null;

	public DungeonManager() {
		this.setName("Dungeon Manager Thread");
		initialize();
	}
	
	/**
	 * Load the skeleton of the dungeon.
	 */
	public void initialize() {
//		RoomGenerator gen = new RoomGenerator(17);
		RoomGenerator gen = new RoomGenerator(1);
		currentRoom = new Room();
		currentRoom.generator = gen;
		gen.generate(currentRoom);
		
		leader = new Swordsman();
		currentRoom.addCharacter(leader);

		final Character friend = new Archer();
		friend.posx = 2;
		friend.posz = 1;
		currentRoom.addCharacter(friend);
		
		final Character enemy = new Ghoul();
		enemy.posx = 2;
		enemy.posz = 2;
		currentRoom.addCharacter(enemy);
		
		final Character enemy2 = new SkeletonWarrior();
		enemy2.posx = 3;
		enemy2.posz = 2;
		currentRoom.addCharacter(enemy2);

		Character enemy3 = new Serpul();
		enemy3.posx = 1;
		enemy3.posz = 2;
		currentRoom.addCharacter(enemy3);

		enemy3 = new Serpul();
		enemy3.posx = 1;
		enemy3.posz = 3;
		currentRoom.addCharacter(enemy3);
	}
	
	public void update(float dt) { // TODO synchronize with touch events that affect the manager
		long time = System.currentTimeMillis();

		if (transitioningPhase) {
			if (time - transitionStartTime >= PHASE_TRANSITION_TIME) {
				transitioningPhase = false;
				guiManager.hidePhaseOverlay();
				becomeTranquil();
			}
			return;
		}

		boolean allWaiting = true;
		Iterator<Character> iterator = currentRoom.characters.iterator();
		while (iterator.hasNext()) {
			Character c = iterator.next();

			if (c.state != Character.STATE_WAITING)
				allWaiting = false;

			if (c.state == Character.STATE_WALKING) {
				c.move(dt);
			} else if (c.state == Character.STATE_TAKING_DAMAGE) {
				if (time - c.stateStartTime >= c.takingDamageAnimationTime) {
					if (c.getHitPoints() == 0)
						c.die();
					else
						c.finishAction();
				}
			} else if (c.state == Character.STATE_DEAD) {
				if (time - c.stateStartTime >= c.deathAnimationTime) {
					iterator.remove(); //remove here to avoid remove while iterating exception
					currentRoom.removeEntity(c);
				}
			} else if (c.state == Character.STATE_ATTACKING) {
				if (!c.stateActionPerformed) {
					if (time - c.stateStartTime >= c.attackHitTime) {
						int damage = c.getStrength();
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
			c.phaseReset();
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
		//temporariy become non-tranquil as a phase indicator is shown on the GUI
		tranquil = false;
		transitioningPhase = true;
		transitionStartTime = System.currentTimeMillis();
		guiManager.showPhaseOverlay(phaseGroup);
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

		Character bestTarget = null;
		LinkedList<int[]> bestPath = null;
		for (Character c : currentRoom.characters) {
			if (c.isPlayerOwned()) {
				LinkedList<int[]> path = currentRoom.findPath(actor.gridRow, actor.gridCol, c.gridRow,
						c.gridCol, false);
				if (path == null)
					continue;
				if (bestPath == null || path.size() < bestPath.size()) {
					bestTarget = c;
					bestPath = path;
				}
			}
		}
		if (bestTarget == null)
			return;

		commandAction(actor, bestPath, Action.basicAttack, bestTarget);
	}

	/**
	 * Commands the actor to walk a path then perform an action on a target.
	 *
	 * path may be null. action may be null. target may be null.
	 *
	 * @param actor the Character that will perform the action
	 * @param path LinkedList of {row, col} int[]s corresponding to grid squares to walk along
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
		if (path != null && path.size() > 0) {
			actor.enqueueAction(action, target);
			actor.walkPath(path);
		} else {
			action.perform(actor, target);
		}
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
			assert path.size() <= actor.getMoveDistance() - actor.getSquaresTraversed();
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

		for (Character c : currentRoom.characters) {
			if (c.actedThisTurn && c.getGroupID() == phaseGroup)
				c.actedMark = true;
		}

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

	/**
	 * Sets the guiManager so this thread can display information
	 *
	 * @param guiManager the current guiManager
	 */
	public void setGUIManager(GUIManager guiManager) {
		this.guiManager = guiManager;
	}

	/**
	 * Checks if the current room is tranquil.
	 *
	 * @return true iff the room is tranquil.
	 */
	public boolean isTranquil() {
		return tranquil;
	}

	//Thread entry point
	public void run() {
		running = true;
		long lastUpdateCall = System.currentTimeMillis();
		while (running) {
			long currentTime;
			synchronized (this) {
				currentTime = System.currentTimeMillis();
				final long timeElapsed = currentTime - lastUpdateCall;
				lastUpdateCall = currentTime;
				update(timeElapsed * .001f);
			}

			currentTime = System.currentTimeMillis();
			long dt = currentTime - lastUpdateCall;
			if (dt < waitMS) {
				try {
					Thread.sleep(waitMS - dt);
				} catch (InterruptedException e) {}
			}

			//display fps measure
			guiManager.displayFPS(dungeonRenderer.fpsMeasure);
		}
	}

}
