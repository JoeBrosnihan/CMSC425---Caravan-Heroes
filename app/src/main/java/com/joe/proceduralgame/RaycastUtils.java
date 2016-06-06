package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.opengl.Matrix;
import android.util.Log;

import java.util.Collection;

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

	/**
	 * Projects a ray onto the plane with altitude 0 and returns the intersection point
	 *
	 * @param result the array in which the result is stored
	 * @param viewMatrix the view matrix of the camera
	 * @param nearX the x intersection of the ray with the plane z = -1 in camera space
	 * @param nearY the y intersection of the ray with the plane z = -1 in camera space
	 */
	public static final void projectOntoGround(float[] result, float[] viewMatrix, float nearX, float nearY) {
		float[] inverseView = new float[16];
		Matrix.invertM(inverseView, 0, viewMatrix, 0);
		float[] eyeAndVec = {0, 0, 0, 1, nearX, nearY, -1, 1};
		float[] transformedVecs = new float[8];
		//transform eye and near vec into camera space
		Matrix.multiplyMV(transformedVecs, 0, inverseView, 0, eyeAndVec, 0);
		Matrix.multiplyMV(transformedVecs, 4, inverseView, 0, eyeAndVec, 4);

		float dy = transformedVecs[5] - transformedVecs[1];
		if (dy != 0) {
			//solve for t
			float t = -transformedVecs[1] / dy;

			//plug back in to find x and z
			float dx = transformedVecs[4] - transformedVecs[0];
			float dz = transformedVecs[6] - transformedVecs[2];
			float projectedX = transformedVecs[0] + dx * t;
			float projectedZ = transformedVecs[2] + dz * t;

			result[0] = projectedX;
			result[1] = projectedZ;
		}
	}

	/**
	 * Finds a quad intersected by the ray from a start point through an aim point
	 *
	 * If a float array of length at least 2 is supplied for result Intersection and if an
	 * intersection is found, the X and Y coordinates of the intersection in the quad's coordinate
	 * frame will be stored at index 0 and 1 respectively. The worldspace coordinates of the
	 * intersection can be retrieved by multiplying the intersected quad's model matrix by the
	 * float[4] vector, {resultIntersection[0], resultIntersection[1], 0, 1}.
	 *
	 * @param resultIntersection an optional float[2] or null to store the quadspace coordinates of an intersection
	 * @param room the Room whose Quads to search
	 * @param quads a RaycastDatastructure containing the Quads to query
	 * @param startX the x coordinate of the origin of the ray
	 * @param startY the Y coordinate of the origin of the ray
	 * @param startZ the Z coordinate of the origin of the ray
	 * @param aimX the x coordinate of a point that the ray will pass through
	 * @param aimY the y coordinate of a point that the ray will pass through
	 * @param aimZ the z coordinate of a point that the ray will pass through
	 * @param betweenPointsOnly if true the raycast will ignore quads not in between the start and aim
	 * @return the closest Quad to the startPoint that is intersected by the ray
	 */
	public static final Quad raycast(float[] resultIntersection, Room room,
	                                 RaycastDatastructure quads, float startX, float startY,
	                                 float startZ, float aimX, float aimY, float aimZ,
	                                 boolean betweenPointsOnly) {
		float[] inverseModel = new float[16]; //transform from world space to quad space
		float[] transformedVecs = new float[8];

		float[] originAndAim = {startX, startY, startZ, 1, aimX, aimY, aimZ, 1};

		//Get horizontal unit vector in direction from start to aim
		float dirX = aimX - startX;
		float dirZ = aimZ - startZ;
		double dist = Math.hypot(dirX, dirZ);
		dirX /= dist;
		dirZ /= dist;

		int currentX = Math.round(startX);
		int currentZ = Math.round(startZ);
		while (true) { //Traverse buckets from start to aim
			int bucketRow = currentZ - room.originz;
			int bucketCol = currentX - room.originx;
			if (bucketRow < 0 || bucketRow >= room.length || bucketCol < 0 || bucketCol >= room.width)
				break; //out of bounds
			Collection<Quad> bucket = quads.buckets[bucketRow][bucketCol];

			//Raycast within current bucket
			for (Quad q : bucket) {
				float[] quadModel = q.modelMatrix;

				Matrix.invertM(inverseModel, 0, quadModel, 0);

				Matrix.multiplyMV(transformedVecs, 0, inverseModel, 0, originAndAim, 0);
				Matrix.multiplyMV(transformedVecs, 4, inverseModel, 0, originAndAim, 4);

				if (betweenPointsOnly) {
					//start and stop must be on different sides of the quad's plane
					if (Math.signum(transformedVecs[6]) == Math.signum(transformedVecs[2]))
						continue;
				}

				float dz = transformedVecs[6] - transformedVecs[2];
				if (dz != 0) {
					float t = -transformedVecs[2] / dz;
					float dx = transformedVecs[4] - transformedVecs[0];
					float dy = transformedVecs[5] - transformedVecs[1];
					float projectedX = transformedVecs[0] + dx * t;
					float projectedY = transformedVecs[1] + dy * t;
					if (-.5f <= projectedX && projectedX < .5f && -.5f <= projectedY && projectedY < .5f) {
						if (resultIntersection != null) {
							resultIntersection[0] = projectedX;
							resultIntersection[1] = projectedY;
						}
						return q;
					}
				}
			}

			//The next candidate buckets are adjacent to the current in the direction of the ray
			int nextHorizontalX = dirX < 0 ? currentX - 1 : currentX + 1;
			int nextVerticalZ = dirZ < 0 ? currentZ - 1 : currentZ + 1;

			float startToHorX = nextHorizontalX - startX;
			float startToHorZ = currentZ - startZ;
			float startToVertX = currentX - startX;
			float startToVertZ = nextVerticalZ - startZ;

			//Project both candidates onto ray to find the closer.
			//Use cross product to find distance from ray.
			float distFromRayToHorNext = dirZ * startToHorX - dirX * startToHorZ;
			float distFromRayToVertNext = dirZ * startToVertX - dirX * startToVertZ;
			if (distFromRayToHorNext < distFromRayToVertNext)
				currentX = nextHorizontalX;
			else
				currentZ = nextVerticalZ;
		}

//		for (int i = 0; i < quadModels.length; i++) {
//			Matrix.invertM(inverseModel, 0, quadModels[i], 0);
//
//			Matrix.multiplyMV(transformedVecs, 0, inverseModel, 0, originAndAim, 0);
//			Matrix.multiplyMV(transformedVecs, 4, inverseModel, 0, originAndAim, 4);
//
//			if (betweenPointsOnly) {
//				//start and stop must be on different sides of the quad's plane
//				if (Math.signum(transformedVecs[6]) == Math.signum(transformedVecs[2]))
//					continue;
//			}
//
//			float dz = transformedVecs[6] - transformedVecs[2];
//			if (dz != 0) {
//				float t = -transformedVecs[2] / dz;
//				float dx = transformedVecs[4] - transformedVecs[0];
//				float dy = transformedVecs[5] - transformedVecs[1];
//				float projectedX = transformedVecs[0] + dx * t;
//				float projectedY = transformedVecs[1] + dy * t;
//				if (-.5f <= projectedX && projectedX < .5f && -.5f <= projectedY && projectedY < .5f) {
//					Quad intersectedQuad = room.staticQuads.get(i);
//					if (intersectedQuad.type == Type.DECORATION)
//						continue;
//
//					if (resultIntersection != null) {
//						resultIntersection[0] = projectedX;
//						resultIntersection[1] = projectedY;
//					}
//					return intersectedQuad;
//				}
//			}
//		}
		return null;
	}

	public static final Quad pick(Room room, float[] viewMatrix, float nearX, float nearY) {
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
		for (Quad q : room.staticQuads) {
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
