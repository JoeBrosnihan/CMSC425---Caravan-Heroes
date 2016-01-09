package com.joe.proceduralgame;

/**
 * Represents an Entity that can be attacked
 */
public abstract class AttackableEntity extends Entity {

    protected AttackableEntity() {
        setDefaultAction(Action.basicAttack);
    }

    /**
     * Called when a Character attacks this entity
     *
     * @param attacker
     * @param damage
     */
    public abstract void takeHit(Character attacker, int damage);

}
