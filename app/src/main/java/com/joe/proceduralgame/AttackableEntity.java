package com.joe.proceduralgame;

/**
 * Represents an Entity that can be attacked
 */
public abstract class AttackableEntity extends Entity {

    protected AttackableEntity() {
        setDefaultAction(Action.basicAttack);
    }

    public abstract void takeDamage(int damage);

}
