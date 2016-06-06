package com.joe.proceduralgame;

import android.opengl.Matrix;

import java.util.HashSet;
import java.util.Collection;

/**
 * Holds quads to allow efficient location based queries
 *
 * If a quad intersects a bucket, it will be placed in that bucket.
 */
public class RaycastDatastructure {

	public final Collection<Quad>[][] buckets;
	private Room room;

	public RaycastDatastructure(Room room) {
		this.room = room;
		buckets = new HashSet[room.length][room.width];
		for (int row = 0; row < room.length; row++) {
			for (int col = 0; col < room.width; col++) {
				buckets[row][col] = new HashSet<>(2);
			}
		}
	}

	public void add(Quad q) {
		float[] corners = {
				.5f, .5f, 0, 1,
				.5f, -.5f, 0, 1,
				-.5f, -.5f, 0, 1,
				-.5f, .5f, 0, 1};
		float[] transformedCorner = new float[4];
		for (int i = 0; i < 4; i++) {
			Matrix.multiplyMV(transformedCorner, 0, q.modelMatrix, 0, corners, 4 * i);
			int row = Math.round(transformedCorner[2] - room.originz);
			int col = Math.round(transformedCorner[0] - room.originx);
			if (row >= 0 && row < room.length && col >= 0 && col < room.width)
				buckets[row][col].add(q);
		}
	}

}
