package com.joe.proceduralgame;

import com.joe.proceduralgame.entities.SolidBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an action a character can perform on an entity.
 *
 * Created for action reusability. Examples of actions include opening a chest, talking to a
 * merchant, pulling a lever, attacking an enemy, etc.
 */
public abstract class Action {

	public enum Visibility {SELECTABLE, NOT_SELECTABLE, HIDDEN};

	public final String name;
	public final int icon_id;
	public final boolean singleTarget;

	/**
	 * Creates a new Action
	 *
	 * @param name the name to display for this Action
	 * @param icon_id the resource id of the icon to display for this Action
	 * @param singleTarget if this Action is targeted at a single entity
	 */
	public Action(String name, int icon_id, boolean singleTarget) {
		this.name = name;
		this.icon_id = icon_id;
		this.singleTarget = singleTarget;
	}

    /**
     * Makes the actor perform this action on the object.
     *
     * This should only be called if canPerform(actor, object) returns true.
     */
    public abstract void perform(Character actor, Entity object);

    /**
     * That is, returns true if the actor can perform this action immediately.
     */
    public abstract boolean canPerform(Character actor, Entity object);

	/**
	 * Returns the visibility of this action in the actor's action pane
	 *
	 * @param actor the selected chatacter
	 * @return the enum Visibility element describing how this action should be displayed
	 */
	public abstract Visibility getVisibility(Character actor);

	/**
	 * Checks if a given grid square is within this action's range.
	 *
	 * @param actor the Character who would act
	 * @param row
	 * @param col
	 * @return true iff the target square is in range
	 */
	public boolean inRange(Character actor, int row, int col) {
		if (actor.currentRoom.grid[row][col] == SolidBlock.singleton)
			return false;
		else
			return true; //default behavior
	}

	/**
	 * Gets all of the grid squares within this action's range.
	 *
	 * @param actor the Character who would act
	 * @return a List of {row, col} int[2] grid squares within range
	 */
	public List<int[]> getRange(Character actor) {
		List<int[]> inRangeSquares = new ArrayList<>();
		for (int row = 0; row < actor.currentRoom.length; row++) {
			for (int col = 0; col < actor.currentRoom.width; col++) {
				if (inRange(actor, row, col)) {
					int[] square = {row, col};
					inRangeSquares.add(square);
				}
			}
		}
		return inRangeSquares;
	}

	/**
	 * Gets the possible targets for this action.
	 *
	 * @param actor the Character who would act
	 * @return a List of Entities that the actor could act upon
	 */
	public List<Entity> getTargets(Character actor) {
		List<Entity> targets = new ArrayList<>();
		List<int[]> range = getRange(actor);
		for (int[] square : range) {
			Entity ent = actor.currentRoom.grid[square[0]][square[1]];
			if (ent != null && canPerform(actor, ent))
				targets.add(ent);
		}
		return targets;
	}

    /**
     * Holds an Action and the Entity target of the action.
     *
     * Used to queue an action to be used on an entity at a later time.
     */
    public static class Pair {
        public final Action action;
        public final Entity target;

        public Pair(Action action, Entity target) {
            this.action = action;
            this.target = target;
        }
    }

    /**
     * Used when any Character attacks an AttackableEntity.
     */
    public static final Action basicAttack = new Action("Attack", R.drawable.attack_action_icon, true) {

        @Override
        public void perform(Character actor, Entity object) {
	        if (object.posx > actor.posx) {
		        actor.dir = 1;
		        if (object instanceof Character)
			        ((Character) object).dir = -1;
	        }
	        if (object.posx < actor.posx) {
		        actor.dir = -1;
		        if (object instanceof Character)
			        ((Character) object).dir = 1;
	        }
            actor.attack((AttackableEntity) object);
        }

        /**
         * @param actor any Character
         * @param object any AttackableEntity
         * @return true iff the actor and the object are in different groups
         */
        @Override
        public boolean canPerform(Character actor, Entity object) {
	        if (!(object instanceof AttackableEntity))
		        return false;
            if (object instanceof Character) {
	            if (actor.getGroupID() != ((Character) object).getGroupID()) {
					return inRange(actor, object.gridRow, object.gridCol);
	            } else {
		            return false;
	            }
            } else {
	            return inRange(actor, object.gridRow, object.gridCol);
            }
        }

	    @Override
	    public Visibility getVisibility(Character actor) {
		    boolean possibleTarget = false;
		    for (Entity e : actor.currentRoom.entities) {
			    if (e instanceof AttackableEntity) {
				    if (e instanceof Character) {
					    if (((Character) e).getGroupID() == actor.getGroupID())
						    continue;
				    }
				    possibleTarget = true;
				    if (canPerform(actor, e))
					    return Visibility.SELECTABLE;
			    }
		    }
		    if (possibleTarget)
		        return Visibility.NOT_SELECTABLE;
		    else
			    return Visibility.HIDDEN;
	    }

	    //Only adjacent squares are in range.
	    @Override
	    public boolean inRange(Character actor, int row, int col) {
		    if (actor.currentRoom.grid[row][col] == SolidBlock.singleton)
			    return false;
		    int diffx = Math.abs(actor.gridRow - row);
		    int diffz = Math.abs(actor.gridCol - col);
		    if (diffx + diffz <= 1)
			    return true;
		    else
			    return false;
	    }

    };

}
