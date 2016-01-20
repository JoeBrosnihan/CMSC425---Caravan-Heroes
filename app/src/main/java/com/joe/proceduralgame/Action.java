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
     * Returns true iff the actor is qualified to perform this action.
     *
     * That is, returns true if the actor could perform this action on this turn, assuming they
     * were in range (i.e. does not check they are in range).
     */
    public abstract boolean canPerform(Character actor, Entity object);

	/**
	 * Returns the visibility of this action in the actor's action pane
	 *
	 * @param actor the selected chatacter
	 * @param manager the active DungeonManager
	 * @return the enum Visibility element describing how this action should be displayed
	 */
	public abstract Visibility getVisibility(Character actor, DungeonManager manager);

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
            actor.attack((AttackableEntity) object);
        }

        /**
         * @param actor any Character
         * @param object any AttackableEntity
         * @return true iff the actor and the object are in different groups
         */
        @Override
        public boolean canPerform(Character actor, Entity object) {
            if (object instanceof Character)
                return actor.getGroupID() != ((Character) object).getGroupID();
            else
                return true;
        }

	    @Override
	    public Visibility getVisibility(Character actor, DungeonManager manager) {
		    return Visibility.SELECTABLE;
	    }
    };

}
