package com.joe.proceduralgame;

import com.joe.proceduralgame.entities.characters.Ghoul;
import com.joe.proceduralgame.entities.characters.SkeletonWarrior;
import com.joe.proceduralgame.entities.characters.Swordsman;

public class DungeonManager extends Thread {

	private TextureManager textureManager;
	
	boolean running = false;
	long waitMS = 10;
	
	Character leader;
	Room currentRoom;
	
	/***
	 * true if the player is in a room free of danger and can act freely.
	 */
	boolean neutral = true;

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
		
		Thread moveDummy = new Thread() {
			public void run() {
				while (true) {
					try {
						enemy.walkTo(4, 2);
						sleep(2000);
						enemy.walkTo(4, 4);
						sleep(2000);
						enemy.walkTo(2, 4);
						sleep(1900);
						enemy.walkTo(2, 2);
						sleep(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		//moveDummy.start();
		
		final Character enemy2 = new SkeletonWarrior();
		enemy2.posx = 3;
		enemy2.posz = 2;
		currentRoom.addCharacter(enemy2);
		
		Thread moveDummy2 = new Thread() {
			public void run() {
				while (true) {
					try {
						enemy2.walkTo(3, 1);
						sleep(2500);
						enemy2.walkTo(3, 3);
						sleep(2000);
						enemy2.walkTo(1, 3);
						sleep(2000);
						enemy2.walkTo(1, 1);
						sleep(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		//moveDummy2.start();
		
	}
	
	public void update(float dt) {
		for (Character c : currentRoom.characters) {
			if (c.state == Character.STATE_WALKING)
				c.move(dt);
		}
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
