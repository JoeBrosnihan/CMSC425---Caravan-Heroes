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
     * @param actor
     * @param object
     */
    public abstract void perform(Character actor, Entity object);

    /**
     * Returns true iff the actor is qualified to perform this action.
     *
     * That is, returns true if the actor could perform this action on this turn, assuming they
     * were in range (i.e. does not check they are in range).
     * @param actor
     * @param object
     * @return
     */
    public abstract boolean canPerform(Character actor, Entity object);

}
