package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.opengl.Matrix;
import android.util.Log;

public class RaycastUtils {
	
	private class Intersection {
		Quad quad;
		float x, y, z;
	}

	/** Returns the character in the room that owns the quad */
	public static Character quadToCharacter(Room room, Quad quad) {
		for (Character c : room.characters) {
			if (c.ownsQuad(quad))
				return c;
		}
		return null;
	}

	/** Returns the entity of the room's entities that owns the quad */
	public static Entity quadToEntity(Room room, Quad quad) {
		for (Entity e : room.entities) {
			if (e.ownsQuad(quad))
				return e;
		}
		return null;
	}
	
	public static Quad pick(Room room, float[] viewMatrix, float nearX, float nearY) {
		float[] inverseModel = new float[16];
		float[] composed = new float[16];
		float[] transformedVecs = new float[8];
		
		float[] inverseView = new float[16];
		Matrix.invertM(inverseView, 0, viewMatrix, 0);
		float[] eyeAndVec = {0, 0, 0, 1, nearX, nearY, -1, 1};
		
		for (Character c : room.characters) {
			Quad q = c.quad;
			Matrix.invertM(inverseModel, 0, q.modelMatrix, 0);
			Matrix.multiplyMM(composed, 0, inverseModel, 0, inverseView, 0);

			Matrix.multiplyMV(transformedVecs, 0, composed, 0, eyeAndVec, 0);
			Matrix.multiplyMV(transformedVecs, 4, composed, 0, eyeAndVec, 4);
			
			float dz = transformedVecs[6] - transformedVecs[2];
			Log.d("touch", "character dz: " + dz);
			if (dz != 0) {
				float t = -transformedVecs[2] / dz;
				float dx = transformedVecs[4] - transformedVecs[0];
				float dy = transformedVecs[5] - transformedVecs[1];
				float projectedX = transformedVecs[0] + dx * t;
				float projectedY = transformedVecs[1] + dy * t;
				if (-.5f <= projectedX && projectedX < .5f && -.5f <= projectedY && projectedY < .5f) {
					float[] intersection = {projectedX, projectedY, 0, 1};
					float[] worldCoords = new float[4];
					Matrix.multiplyMV(worldCoords, 0, q.modelMatrix, 0, intersection, 0);
					
					return q;
				}
			}
		}
		for (Quad q : room.quads) {
			if (q.type == Type.DECORATION)
				continue;
			Matrix.invertM(inverseModel, 0, q.modelMatrix, 0);
			Matrix.multiplyMM(composed, 0, inverseModel, 0, inverseView, 0);

			Matrix.multiplyMV(transformedVecs, 0, composed, 0, eyeAndVec, 0);
			Matrix.multiplyMV(transformedVecs, 4, composed, 0, eyeAndVec, 4);
			
			float dz = transformedVecs[6] - transformedVecs[2];
			if (dz != 0) {
				float t = -transformedVecs[2] / dz;
				float dx = transformedVecs[4] - transformedVecs[0];
				float dy = transformedVecs[5] - transformedVecs[1];
				float projectedX = transformedVecs[0] + dx * t;
				float projectedY = transformedVecs[1] + dy * t;
				if (-.5f <= projectedX && projectedX < .5f && -.5f <= projectedY && projectedY < .5f) {
					float[] intersection = {projectedX, projectedY, 0, 1};
					float[] worldCoords = new float[4];
					Matrix.multiplyMV(worldCoords, 0, q.modelMatrix, 0, intersection, 0);
					
					return q;
				}
			}
		}
		return null;
	}

}
