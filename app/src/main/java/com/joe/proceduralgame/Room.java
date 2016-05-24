package com.joe.proceduralgame;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class Room {

	public RoomGenerator generator;
	/**
	 * Entities should only be added to the grid through addEntity(). Their position on the grid
	 * can be changed by calling moveEntity().
	 */
	Entity[][] grid;
	EdgeEntity[] edges;
	int width, length;
	int originx, originz;

	//Holds the lights of this room.
	RoomLighting lighting;
	//Holds only the static staticQuads of this room.
	//These quads are loaded by the RoomGenerator. After load, none are added or transformed.
	List<Quad> staticQuads = new ArrayList<Quad>();

	List<Character> characters = new ArrayList<Character>();
	List<Entity> entities = new ArrayList<Entity>();
	List<EdgeEntity> edgeEntities = new ArrayList<EdgeEntity>();

	List<DamageDisplay> damageDisplays = new ArrayList<DamageDisplay>();

	////// static geometry //////
	// sorted first by texture then by uv patch
	float[][] quadModels; // model matrix of each quad
	int[] textureIndices; // indices for change of texture
	int[] uvIndices; // indices for change of uv patch
	int[] textures; // texture units for each texture index
	float[][] uvOrigins; // {u, v} origin for each uv index
	float[][] uvScales; // size of patch on texture for each uv index
	////// end static geometry //////

	public void moveEntity(Entity entity, int fromRow, int fromCol) {
		// XXX What if a team member moves through a team member?
		assert grid[fromRow][fromCol] == entity;
		assert grid[entity.gridRow][entity.gridCol] == null;
		grid[fromRow][fromCol] = null;
		grid[entity.gridRow][entity.gridCol] = entity;
	}

	/**
	 * Removes an entity from this room.
	 *
	 * @param entity the entity to remove
	 */
	public void removeEntity(Entity entity) {
		assert grid[entity.gridRow][entity.gridCol] == entity;
		grid[entity.gridRow][entity.gridCol] = null;
		entities.remove(entity);
		if (entity instanceof Character)
			characters.remove(entity);
	}
	
	/**
	 * Places entity on the grid, adds it to the room's entities, sets entity.currentRoom to this Room.
	 * The entity must have already been positioned to not overlap anything. 
	 * @param entity
	 */
	public void addEntity(Entity entity) {
		entity.gridRow = Math.round(entity.posz - originz);
		entity.gridCol = Math.round(entity.posx - originx);
		assert grid[entity.gridRow][entity.gridCol] == null;
		grid[entity.gridRow][entity.gridCol] = entity;
		entities.add(entity); //TODO handle synchronous array operations. Sycn the array, use CopyableArray, or something else.
		entity.currentRoom = this;
	}
	
	/**
	 * Places entity on the approriate edge, adds it to the room's edgeEntities, sets entity.currentRoom to this Room.
	 * The entity must have already been positioned to not overlap anything. 
	 * @param entity
	 */
	public void addEdgeEntity(EdgeEntity entity) {
		int index;
		if (entity.dir % 2 == 0)
			index = (int) Math.round((2 * width + 1) * (entity.posz - originz) + width + entity.posx - originx + .5);
		else
			index = Math.round((2 * width + 1) * (entity.posz - originz + .5f) + entity.posx - originx);
		entity.edgesIndex = index;
		
		assert edges[index] == null;
		edges[index] = entity;
		edgeEntities.add(entity); //TODO handle synchronous array operations. Sycn the array, use CopyableArray, or something else.
		entity.currentRoom = this;
	}
	
	/**
	 * Returns the edge index corresponding to the given position.
	 * @param posx
	 * @param posz
	 * @return
	 */
	public int edgeIndexAt(float posx, float posz) {
		float relx = posx - originx;
		float relz = posz - originz;
		if (relx % 1 > .25f && relx % 1 < .75f) // dir = 0
			return (int) Math.round((2 * width + 1) * (posz - originz) + width + posx - originx + .5);
		else
			return Math.round((2 * width + 1) * (posz - originz + .5f) + posx - originx);
	}

	public void addDamageDisplay(DamageDisplay display) {
		damageDisplays.add(display);
	}

	public void addCharacter(Character c) {
		characters.add(c);
		addEntity(c);
	}
	
	public void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion { //TODO unload rooms
		lighting = new RoomLighting(this);
		generator.load(this);
		DungeonRenderer.catchGLError();
		loadStaticGeometry(tex);
		DungeonRenderer.catchGLError();
		for (Entity e : entities) {
			e.graphicLoad(tex);
			DungeonRenderer.catchGLError();
		}
		for (EdgeEntity e : edgeEntities) {
			e.load(tex);
			DungeonRenderer.catchGLError();
		}
		lighting.load(tex);
		return;
	}
	
	private int hashUV(float[] uvOrigin, float[] uvScale) {
		return (int) (uvScale[0] + 128 * (uvScale[1] + 128 * (uvOrigin[0] + 128 * uvOrigin[1])));
	}
	
	private void loadStaticGeometry(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		if(staticQuads.isEmpty()) {
			//initialize
			return;
		}
		
		// sort staticQuads first by texture, then by uv patch within a texture
		Collections.sort(staticQuads, new Comparator<Quad>() {
			public int compare(Quad lhs, Quad rhs) {
				int comp = lhs.textureID - rhs.textureID;
				if (comp != 0) {
					return comp;
				} else {
					return hashUV(lhs.uvOrigin, lhs.uvScale) - hashUV(rhs.uvOrigin, lhs.uvScale);
				}
			}
		});
		
		int currentTextureID = -1;
		int currentUVHash = -1;
		// set not equal for convenience
		currentTextureID = staticQuads.get(0).textureID - 1;
		currentUVHash = hashUV(staticQuads.get(0).uvOrigin, staticQuads.get(0).uvScale) - 1;
			
		//count size for tex and uv arrays
		int nUniqueTextures = 0;
		int nUniqueUVs = 0;
		for (int i = 0; i < staticQuads.size(); i++) {
			Quad q = staticQuads.get(i);
			if (q.textureID != currentTextureID) {
				nUniqueTextures++;
				currentTextureID = q.textureID;
			}
			if (hashUV(q.uvOrigin, q.uvScale) != currentUVHash) {
				nUniqueUVs++;
				currentUVHash = hashUV(q.uvOrigin, q.uvScale);
			}
		}
		quadModels = new float[staticQuads.size()][16];
		textureIndices = new int[nUniqueTextures];
		uvIndices = new int[nUniqueUVs];
		textures = new int[nUniqueTextures];
		uvOrigins = new float[nUniqueUVs][2];
		uvScales = new float[nUniqueUVs][2];
		
		// set not equal to the first element for convenience
		currentTextureID = staticQuads.get(0).textureID - 1;
		currentUVHash = hashUV(staticQuads.get(0).uvOrigin, staticQuads.get(0).uvScale) - 1;

		// current slots in index arrays
		int iTex = -1;
		int iUV = -1;
		for (int i = 0; i < staticQuads.size(); i++) {
			Quad q = staticQuads.get(i);
			// copy model matrix
			quadModels[i] = q.modelMatrix;
			
			if (q.textureID != currentTextureID) {
				iTex++;
				currentTextureID = q.textureID;
				textures[iTex] = tex.referenceLoad(currentTextureID);
				textureIndices[iTex] = i;
			}
			
			if (hashUV(q.uvOrigin, q.uvScale) != currentUVHash) {
				iUV++;
				currentUVHash = hashUV(q.uvOrigin, q.uvScale);
				uvOrigins[iUV] = q.uvOrigin;
				uvScales[iUV] = q.uvScale; // assume square
				uvIndices[iUV] = i;
			}
			
		}
		
	}

	/**
	 * Draws only the static geometry of the room. No Entities.
	 * @param shaderProgram the OpenGL name of the shader to use
	 * @param mVPMatrix the view projection matrix to use
	 */
	public void draw(int shaderProgram, float[] mVPMatrix) {
		int modelHandle = GLES20.glGetUniformLocation(shaderProgram, "modelMatrix");
		int mvpHandle = GLES20.glGetUniformLocation(shaderProgram, "MVP");
		int textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
		int originHandle = GLES20.glGetUniformLocation(shaderProgram, "uvOrigin");
		int scaleHandle = GLES20.glGetUniformLocation(shaderProgram, "uvScale");

		int lightmapHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightmap");
		GLES20.glUniform1i(lightmapHandle, TextureManager.LIGHTMAP_UNIT);
		int lightmapScaleHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightmapScale");
		GLES20.glUniform1f(lightmapScaleHandle, lighting.mapColumnWidth);
		int lightmapUVHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightmapUV");
		
		int iTex = 0;
		int iUV = 0;
		int textureChangeIndex = textureIndices[iTex];
		int uvChangeIndex = uvIndices[iUV];
		
		float[] mvp = new float[16];
		for (int i = 0; i < quadModels.length; i++) {
			Quad.enableArrays(shaderProgram);

			// change quad model matrix
			Matrix.multiplyMM(mvp, 0, mVPMatrix, 0, quadModels[i], 0);
			GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
			GLES20.glUniformMatrix4fv(modelHandle, 1, false, quadModels[i], 0);

			// change quad lightmap uv
			float lightmapU = TextureManager.atlasU(i, lighting.nMapColumns);
			float lightmapV = TextureManager.atlasV(i, lighting.nMapColumns);
			GLES20.glUniform2f(lightmapUVHandle, lightmapU, lightmapV);

			if (i == textureChangeIndex) {
				// change texture
				GLES20.glUniform1i(textureHandle, textures[iTex]);
				
				iTex++;
				if (iTex < textureIndices.length)
					textureChangeIndex = textureIndices[iTex];
			}
			if (i == uvChangeIndex) {
				// change uv
				GLES20.glUniform2f(originHandle, uvOrigins[iUV][0], uvOrigins[iUV][1]);
				GLES20.glUniform2f(scaleHandle, uvScales[iUV][0], uvScales[iUV][1]);
				
				iUV++;
				if (iUV < uvIndices.length)
					uvChangeIndex = uvIndices[iUV];
			}

			//draw
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
			Quad.disableArrays(shaderProgram);
		}
	}

	public void destroy() {

	}

	/**
	 * Finds the closest empty square to the start that is adjacent to the end square.
	 *
	 * Returns [startRow, startCol] if the start square is adjacent to the end square.
	 * Returns null if there is no open adjacent square.
	 *
	 * @return an int[] = {row, col} of the closest square or null
	 */
	public int[] getClosestAdjacentSquare(int startRow, int startCol, int endRow, int endCol) {
		int[] result = null;
		double resultDist = Double.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			double theta = Math.PI * .5 * i;
			int adjRow = (int) Math.round(endRow - Math.sin(theta));
			int adjCol = (int) Math.round(endCol + Math.cos(theta));

			if ((startRow == adjRow && startCol == adjCol) || grid[adjRow][adjCol] == null) {
				double dist = Math.hypot(adjRow - startRow, adjCol - startCol);
				if (dist < resultDist) {
					resultDist = dist;
					result = new int[2];
					result[0] = adjRow;
					result[1] = adjCol;
				}
			}
		}
		return result;
	}

	/**
	 * Finds a shortest path in this room between two squares that avoids squares with Entities.
	 *
	 * Excludes the starting square, includes the destination square. Returns a list of length 0 if
	 * the start square is the end square.
	 *
	 * @param startRow
	 * @param startCol
	 * @param destRow
	 * @param destCol
	 * @param includeDest if true, the path will exclude the dest square and will end on an
	 *                    adjacent square.
	 * @return a LinkedList of length 2 int[]s, each corresponding to the row and col of the next
	 * square, or returns null if no such path exists.
	 */
	public LinkedList<int[]> findPath(int startRow, int startCol, final int destRow,
									  final int destCol, boolean includeDest) {
		boolean[][] discovered = new boolean[length][width]; //initialized to false
		final int[][] g = new int[length][width];
		int[][][] prev = new int[length][width][];

		Comparator<int[]> squareComparator = new Comparator<int[]>() {
			@Override
			public int compare(int[] lhs, int[] rhs) {
				double h_lhs = Math.hypot(destRow - lhs[0], destCol - lhs[1]);
				double h_rhs = Math.hypot(destRow - rhs[0], destCol - rhs[1]);
				double f_lhs = g[lhs[0]][lhs[1]] + h_lhs;
				double f_rhs = g[rhs[0]][rhs[1]] + h_rhs;
				if (f_lhs > f_rhs)
					return 1;
				else if (f_lhs < f_rhs)
					return -1;
				else return
					0;
			}
		};

		PriorityQueue<int[]> frontier = new PriorityQueue<int[]>(4, squareComparator);

		int[] curSquare = new int[2];
		curSquare[0] = startRow;
		curSquare[1] = startCol;
		frontier.add(curSquare);

		Outer:
		while ((curSquare = frontier.poll()) != null) {
			if (curSquare[0] == destRow && curSquare[1] == destCol)
				break;

			for (int i = 0; i < 4; i++) {
				double theta = Math.PI * .5 * i;
				int adjRow = (int) Math.round(curSquare[0] - Math.sin(theta));
				int adjCol = (int) Math.round(curSquare[1] + Math.cos(theta));
				if (adjRow < 0 || adjRow >= length || adjCol < 0 || adjCol >= width)
					continue;
				if (!discovered[adjRow][adjCol]) {
					discovered[adjRow][adjCol] = true;
					if (!includeDest && adjRow == destRow && adjCol == destCol)
						break Outer;
					if (grid[adjRow][adjCol] != null)
						continue;
					//set ptr to prev square
					prev[adjRow][adjCol] = curSquare;
					//cost so far
					g[adjRow][adjCol] = g[curSquare[0]][curSquare[1]] + 1;

					int[] adjSquare = new int[2];
					adjSquare[0] = adjRow;
					adjSquare[1] = adjCol;
					frontier.add(adjSquare);
				}
			}
		}

		if (curSquare != null) { //success, reconstruct path
			LinkedList<int[]> path = new LinkedList<int[]>();
			while (curSquare != null && !(curSquare[0] == startRow && curSquare[1] == startCol)) {
				path.addFirst(curSquare);
				curSquare = prev[curSquare[0]][curSquare[1]];
			}
			return path;
		} else {
			return null;
		}
	}

	/**
	 * Gets all squares reachable within range # of hops from a start square
	 *
	 * @param startRow the row of the start square
	 * @param startCol the column of the start square
	 * @param range the maximum length of a path from the start square
	 * @return an array of {row, col} int[]s representing all reachable squares
	 */
	public int[][] getRange(int startRow, int startCol, int range) {
		boolean[][] discovered = new boolean[length][width]; //initialized to false
		boolean[][] reachable = new boolean[length][width]; //initialized to false
		final int[][] g = new int[length][width]; //initialized to zero
		int nReachable = 0;

		Queue<int[]> frontier = new LinkedList<int[]>();

		int[] curSquare = new int[2];
		curSquare[0] = startRow;
		curSquare[1] = startCol;
		frontier.add(curSquare);

		Outer:
		while ((curSquare = frontier.poll()) != null) {
			reachable[curSquare[0]][curSquare[1]] = true;
			nReachable++;
			if (g[curSquare[0]][curSquare[1]] == range)
				continue;
			for (int i = 0; i < 4; i++) {
				double theta = Math.PI * .5 * i;
				int adjRow = (int) Math.round(curSquare[0] - Math.sin(theta));
				int adjCol = (int) Math.round(curSquare[1] + Math.cos(theta));
				if (adjRow < 0 || adjRow >= length || adjCol < 0 || adjCol >= width) //check in bounds
					continue;
				if (!discovered[adjRow][adjCol]) { //if not discovered
					discovered[adjRow][adjCol] = true;
					if (grid[adjRow][adjCol] != null)
						continue;

					//cost so far
					g[adjRow][adjCol] = g[curSquare[0]][curSquare[1]] + 1;

					int[] adjSquare = new int[2];
					adjSquare[0] = adjRow;
					adjSquare[1] = adjCol;
					frontier.add(adjSquare);
				}
			}
		}

		int[][] options = new int [nReachable][2];
		int index = 0;
		for (int row = 0; row < length; row++) {
			for (int col = 0; col < width; col++) {
				if (reachable[row][col]) {
					options[index][0] = row;
					options[index][1] = col;
					index++;
				}
			}
		}

		return options;
	}

}
