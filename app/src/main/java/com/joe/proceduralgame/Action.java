package com.joe.proceduralgame;

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

	/**
	 * Creates a new Action
	 *
	 * @param name the name to display for this Action
	 * @param icon_id the resource id of the icon to display for this Action
	 */
	public Action(String name, int icon_id) {
		this.name = name;
		this.icon_id = icon_id;
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
    public static final Action basicAttack = new Action("Attack", R.drawable.attack_action_icon) {

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
					return inRange(actor, object);
	            } else {
		            return false;
	            }
            } else {
	            return inRange(actor, object);
            }
        }

	    @Override
	    public Visibility getVisibility(Character actor) {
		    boolean possibleTarget = false;
		    for (Entity e : actor.currentRoom.entities) {
			    if (e instanceof AttackableEntity) {
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

	    private boolean inRange(Character actor, Entity object) {
		    return Math.abs(actor.gridRow - object.gridRow) + Math.abs(actor.gridCol - object.gridCol) <= 1;
	    }

    };

}
