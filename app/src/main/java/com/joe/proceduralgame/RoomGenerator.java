package com.joe.proceduralgame;

import java.util.Random;

import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.entities.Door;
import com.joe.proceduralgame.entities.SolidBlock;

import android.opengl.Matrix;

public class RoomGenerator {
	
	public static final float STANDARD_OBJECT_TILT_ANGLE = 45;
	Random rand;
	
	public RoomGenerator(long seed) {
		rand = new Random(seed);
	}
	
	public void generate(Room room) {
		room.width = rand.nextInt(10) + 7;
		room.length = rand.nextInt(5) + 5;
		room.grid = new Entity[room.length][room.width];
		room.edges = new EdgeEntity[(2 * room.width + 1) * room.length + room.width];
		for (int row = 1; row < room.length - 1; row++) {
			for (int col = 1; col < room.width - 1; col++) {
				if (rand.nextFloat() < .1f)
					room.grid[row][col] = SolidBlock.singleton;
			}
		}
		
		//gen doors
		int dir = EdgeEntity.DIR_HORIZONTAL;
		for (int col = 0; col < room.width - 1; col++) { // TODO generate doors smarter and so they don't overlap solid blocks
			int row = 0;
			if (rand.nextFloat() < .1) {
				Door door = new Door();
				door.dir = dir;
				door.posx = room.originx + col;
				door.posz = room.originz + row - .5f;
				room.addEdgeEntity(door);
			}
			row = room.length;
			if (rand.nextFloat() < .1) {
				Door door = new Door();
				door.dir = dir;
				door.posx = room.originx + col;
				door.posz = room.originz + row - .5f;
				room.addEdgeEntity(door);
			}
		}
		dir = EdgeEntity.DIR_VERTICAL;
		for (int row = 0; row < room.length - 1; row++) { // TODO generate doors smarter and so they don't overlap solid blocks
			int col = 0;
			if (rand.nextFloat() < .1) {
				Door door = new Door();
				door.dir = dir;
				door.posx = room.originx + col - .5f;
				door.posz = room.originz + row;
				room.addEdgeEntity(door);
			}
			col = room.width;
			if (rand.nextFloat() < .1) {
				Door door = new Door();
				door.dir = dir;
				door.posx = room.originx + col - .5f;
				door.posz = room.originz + row;
				room.addEdgeEntity(door);
			}
		}
	}
	
	public void load(Room room) {
		for (int row = 0; row < room.length; row++) {
			for (int col = 0; col < room.width; col++) {
				if (room.grid[row][col] != SolidBlock.singleton) {
					int floorTile = R.drawable.dungeonfloortile1;
					if (rand.nextFloat() < .5f) {
						switch (rand.nextInt(4)) {
						case 0:
							floorTile = R.drawable.dirt;
							break;
						case 1:
							floorTile = R.drawable.dungeonfloortile1_1;
							break;
						case 2:
							floorTile = R.drawable.dungeonfloortile1_2;
							break;
						case 3:
							floorTile = R.drawable.dungeonfloortile2;
							break;
						}
					}
					addFloorQuad(room, row, col, floorTile);
					if (rand.nextFloat() < .1f) {
						int item = rand.nextInt(13);
						addDecorationObject(room, row, col, R.drawable.objectsatlas1, item, 4);
					}
					
					for (int i = 0; i < 4; i++) {
						double theta = Math.PI * .5 * i;
						int adjRow = (int) Math.round(row - Math.sin(theta));
						int adjCol = (int) Math.round(col + Math.cos(theta));

						float posx = room.originx + (col + adjCol) * .5f;
						float posz = room.originz + (row + adjRow) * .5f;
						
						// load walls (if there's no door or anything already there)
						if (room.edges[room.edgeIndexAt(posx, posz)] == null) {
							if (adjRow < 0 || adjRow >= room.length || adjCol < 0 || adjCol >= room.width) {
								addWallQuad(room, row, col, i, R.drawable.dungeonwalltile3);
								if (rand.nextFloat() < .1f)
									addTorch(room, row, col, i, R.drawable.objtorch2);
									///
								continue;
							}
							if (room.grid[adjRow][adjCol] == SolidBlock.singleton) {
								addWallQuad(room, row, col, i, R.drawable.dungeonwalltile2);
								if (rand.nextFloat() < .1f)
									addTorch(room, row, col, i, R.drawable.objtorch2);
							}
						}
					}
				}
			}
		}
	}

	private void addFloorQuad(Room room, int row, int col, int texture) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, room.originx + col, 0, room.originz + row);
		Matrix.rotateM(model, 0, 90, 1, 0, 0);
		Quad floor = Quad.createStaticQuad(Type.FLOOR, model, texture);
		room.staticQuads.add(floor);
	}

	private void addTorch(Room room, int row, int col, int dir, int texture) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, room.originx + col, 0, room.originz + row);
		Matrix.rotateM(model, 0, 90 + dir * 90, 0, 1, 0);
		Matrix.translateM(model, 0, 0, .75f, .45f);
		Matrix.scaleM(model, 0, .3f, .3f, 0);
		Quad wall = Quad.createStaticQuad(Type.DECORATION, model, texture);
		room.staticQuads.add(wall);

		float[] origin = {0, 0, 0.05f, 1};
		float[] lightPos = new float[4];
		Matrix.multiplyMV(lightPos, 0, model, 0, origin, 0);

		RoomLighting.StaticLightBody light = new RoomLighting.LightSphere(lightPos[0], lightPos[1], lightPos[2], .1f, .7f, .7f, .5f);
		room.lighting.addStaticLight(light);
	}

	private void addWallQuad(Room room, int row, int col, int dir, int texture) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, room.originx + col, 0, room.originz + row);
		Matrix.rotateM(model, 0, 90 + dir * 90, 0, 1, 0);
		Matrix.translateM(model, 0, 0, .5f, .5f);
		Quad wall = Quad.createStaticQuad(Type.WALL, model, texture);
		room.staticQuads.add(wall);
	}
	
	private void addDecorationObject(Room room, int row, int col, int texture, int atlasIndex, int atlasSize) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, room.originx + col, 0, room.originz + row);
		Matrix.rotateM(model, 0, -STANDARD_OBJECT_TILT_ANGLE, 1, 0, 0);
		Matrix.scaleM(model, 0, -.5f, .5f, .5f);
		Matrix.translateM(model, 0, 0, .45f, 0);
		Quad obj = Quad.createStaticQuad(Type.DECORATION, model, texture);
		obj.uvOrigin[0] = TextureManager.atlasU(atlasIndex, atlasSize);
		obj.uvOrigin[1] = TextureManager.atlasV(atlasIndex, atlasSize);
		obj.uvScale[0] = 1 / (float) atlasSize;
		obj.uvScale[1] = 1 / (float) atlasSize;
		room.staticQuads.add(obj);
	}

}
