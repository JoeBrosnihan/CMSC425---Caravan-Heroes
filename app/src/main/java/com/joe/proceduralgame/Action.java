package com.joe.proceduralgame;

/**
 * Represents an action a character can perform on an entity.
 *
 * Created for action reusability. Examples of actions include opening a chest, talking to a
 * merchant, pulling a lever, attacking an enemy, etc.
 */
public abstract class Action {
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
    public static final Action basicAttack = new Action() {

        @Override
        public void perform(Character actor, Entity object) {
            actor.attack((AttackableEntity) object);
        }

        /**
         * @param actor any Character
         * @param object any AttackableEntity
         * @return true iff not both actor and object are player units
         */
        @Override
        public boolean canPerform(Character actor, Entity object) {
            if (object instanceof Character)
                //verify not both units are the player's
                return !(actor.isPlayerOwned() && ((Character) object).isPlayerOwned());
            else
                return true;
        }
    };

}
