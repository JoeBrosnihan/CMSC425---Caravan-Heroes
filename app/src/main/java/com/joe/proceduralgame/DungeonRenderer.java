package com.joe.proceduralgame;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class DungeonRenderer implements GLSurfaceView.Renderer {

	public static final float NEAR_Z = 1;
	public static final float CAMERA_ANGLE = 60;
	public static final float DEFAULT_FOV = 45;
	public static final float DEFAULT_NEAR_HEIGHT = (float) Math.tan(Math.toRadians(DEFAULT_FOV / 2));
	//The fraction of the distance to the focus covered in one second
	public static final float FOCUS_CAM_SPEED = .90f, PANNING_CAM_SPEED = .99f;
	
	private GameGLView gameView;
	private TextureManager textureManager;
	Controller gameController;
	
	private int program;
	private DungeonManager dungeonManager;
	private FloatBuffer vertexBuffer;
	private final float[] mVPMatrix = new float[16];
	private final float[] mProjectionMatrix = new float[16];
	final float[] mViewMatrix = new float[16];
	public float nearWidth, nearHeight;
	float camx, camy = 8, camz;
	float destx, destz;
	private Entity focus;
	private long lastDrawTime;
	
	private Quad characterSelector;
	private int uiTextureUnit;
	/** Quads used to highlight squares a character can move to this turn. May be null */
	private Quad[] moveOptionQuads = null;
	private Quad[] attackOptionQuads = null;
	
	public static void catchGLError() {
		int a1 = GLES20.GL_INVALID_ENUM;
		int a2 = GLES20.GL_INVALID_FRAMEBUFFER_OPERATION;
		int a3 = GLES20.GL_INVALID_OPERATION;
		int a4 = GLES20.GL_INVALID_VALUE;
		int e = GLES20.glGetError();
		if (e != 0) {
			Log.e("game", "Error code: " + e);
		}
	}
	
	public DungeonRenderer(GameGLView view, DungeonManager dungeonManager, TextureManager textureManager) {
		this.gameView = view;
		this.textureManager = textureManager;
		this.dungeonManager = dungeonManager;
	}
	
	public void draw() {
	    GLES20.glUseProgram(program);

		int texturelessHandle = GLES20.glGetUniformLocation(program, "uTextureless");
		GLES20.glUniform1i(texturelessHandle, 0); //draw all quads with textures by default
		int colorMultiplierHandle = GLES20.glGetUniformLocation(program, "uColorMultiplier");
		GLES20.glUniform4f(colorMultiplierHandle, 1, 1, 1, 1); //draw all quads without color modification by default

		//Update lighting
		if (dungeonManager.currentRoom.lighting.needsUpdate())
			dungeonManager.currentRoom.lighting.update(program);

		//Draw static room geometry
	    GLES20.glEnable(GLES20.GL_CULL_FACE);
	    dungeonManager.currentRoom.draw(program, mVPMatrix, vertexBuffer);
    	catchGLError();

		//Draw box around selected Character
    	if (gameController.selectedCharacter != null) {
    		float posx = Math.round(gameController.selectedCharacter.posx); // TODO Could be optimiized to only do these calcs when nec.
    		float posz = Math.round(gameController.selectedCharacter.posz);
    		Matrix.setIdentityM(characterSelector.modelMatrix, 0);
    		Matrix.translateM(characterSelector.modelMatrix, 0, posx, .1f, posz);
    		Matrix.rotateM(characterSelector.modelMatrix, 0, 90, 1, 0, 0);
    		characterSelector.draw(program, mVPMatrix);
    	}

		//Highlight reachable squares
		if (moveOptionQuads != null) {
			GLES20.glUniform1i(texturelessHandle, 1); //draw all quads blank white
			GLES20.glUniform4f(colorMultiplierHandle, 81 / 255.f, 145 / 255.f, 255 / 255.f, .5f);
			for (Quad q : moveOptionQuads) {
				q.drawWithoutTexture(program, mVPMatrix);
			}
			GLES20.glUniform1i(texturelessHandle, 0);
			GLES20.glUniform4f(colorMultiplierHandle, 1, 1, 1, 1);
		}
		if (attackOptionQuads != null) {
			for (Quad q : attackOptionQuads) {
				q.draw(program, mVPMatrix);
			}
		}
    	
	    GLES20.glDisable(GLES20.GL_CULL_FACE);
		//TODO do these need to be synchronized as well? What if something gets added from game thread?
	    for (EdgeEntity e : dungeonManager.currentRoom.edgeEntities) {
	    	e.draw(program, mVPMatrix);
	    }
	    for (Entity e : dungeonManager.currentRoom.entities) {
	    	e.draw(program, mVPMatrix);
	    }
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		synchronized (dungeonManager.currentRoom.damageDisplays) {
			long t = System.currentTimeMillis();
			Iterator<DamageDisplay> iter = dungeonManager.currentRoom.damageDisplays.iterator();
			while (iter.hasNext()) {
				DamageDisplay display = iter.next();
				if (t - display.creationTime >= DamageDisplay.LIFETIME)
					iter.remove();
				else
					display.draw(program, mVPMatrix, uiTextureUnit);
			}
		}
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	}

	public void setFocus(Entity focus) {
		synchronized (this) {
			this.focus = focus;
		}
	}

	public void onDrawFrame(GL10 unused) {
		long time = System.currentTimeMillis();
		double dt = (time - lastDrawTime) / 1000.0;
		// Set the camera position (View matrix)
		updateCamera(dt);
	    
	    Matrix.setIdentityM(mViewMatrix, 0);
	    Matrix.translateM(mViewMatrix, 0, 0, 0, -camy);
	    Matrix.rotateM(mViewMatrix, 0, CAMERA_ANGLE, 1, 0, 0);
	    Matrix.translateM(mViewMatrix, 0, -camx, 0, -camz);

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

		//Spin the attack option selectors
		if (attackOptionQuads != null) {
			for (Quad q : attackOptionQuads) {
				Matrix.rotateM(q.modelMatrix, 0, (float) dt * 180.0f, 0, 0, 1);
			}
		}
		
	    catchGLError();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    catchGLError();
		
		draw();
	    catchGLError(); //TODO use these for testing, then clean up

		lastDrawTime = time;
	}

	/**
	 * Highlights squares to indicate where a character can move
	 *
	 * @param squares an array of {row, col} int[]s of all the squares to highlight
	 */
	public void showMoveOptions(int[][] squares) {
		moveOptionQuads = new Quad[squares.length];
		for (int i = 0; i < squares.length; i++) {
			float[] modelMatrix = new float[16];
			float posx = dungeonManager.currentRoom.originx + squares[i][1];
			float posz = dungeonManager.currentRoom.originz + squares[i][0];
			Matrix.setIdentityM(modelMatrix, 0);
			Matrix.translateM(modelMatrix, 0, posx, .05f, posz);
			Matrix.rotateM(modelMatrix, 0, 90, 1, 0, 0);
			Quad quad = Quad.createDynamicQuad(Type.DECORATION, modelMatrix, 0);
			moveOptionQuads[i] = quad;
		}
	}

	/**
	 * Marks the targets for possible attack
	 *
	 * @param targets the Entities to mark
	 */
	public void showAttackOptions(List<Entity> targets) {
		attackOptionQuads = new Quad[targets.size()];
		int i = 0;
		for (Entity t : targets) {
			float[] modelMatrix = new float[16];
			Matrix.setIdentityM(modelMatrix, 0);
			Matrix.translateM(modelMatrix, 0, t.posx, .05f, t.posz);
			Matrix.rotateM(modelMatrix, 0, 90, 1, 0, 0);
			Matrix.scaleM(modelMatrix, 0, .8f, .8f, 1);
			Quad q = Quad.createDynamicQuad(Type.DECORATION, modelMatrix, uiTextureUnit);
			q.uvOrigin[0] = 4f / 8f;
			q.uvOrigin[1] = 2f / 8f;
			q.uvScale[0] = 1f / 8f;
			q.uvScale[1] = 1f / 8f;
			attackOptionQuads[i++] = q;
		}
	}

	/**
	 * Hides any marks for possible attacks
	 */
	public void hideAttackOptions() {
		attackOptionQuads = null;
	}

	/**
	 * Stops highlighting squares indicating where a character can move
	 * May be called if no squares are highlighted
	 */
	public void hideMoveOptions() {
		moveOptionQuads = null;
	}

	/**
	 * Adjusts the camera's position
	 *
	 * @param dt time elapsed since last frame in seconds
	 */
	private void updateCamera(double dt) {
		float deltax, deltaz;
		float factor;
		synchronized (this) {
			if (focus != null) {
				factor = (float) Math.pow(1 - FOCUS_CAM_SPEED, dt);
				destx = focus.posx;
				destz = focus.posz;
			} else {
				factor = (float) Math.pow(1 - PANNING_CAM_SPEED, dt);
			}
		}
		deltax = destx - camx;
		deltaz = destz - camz;
		double dist = Math.hypot(deltax, deltaz);
		if (dist < .001f) {
			camx += deltax;
			camz += deltaz;
		} else {
			camx += deltax * (1 - factor);
			camz += deltaz * (1 - factor);
		}
	}
	
	private void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		Quad.loadBuffer();

		uiTextureUnit = tex.referenceLoad(R.drawable.ui_atlas);
		characterSelector = Quad.createDynamicQuad(Type.DECORATION, new float[16], uiTextureUnit);
		characterSelector.uvOrigin[0] = 3f / 8f;
		characterSelector.uvOrigin[1] = 0f;
		characterSelector.uvScale[0] = 1f / 8f;
		characterSelector.uvScale[1] = 1f / 8f;
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		float ratio = width / (float) height;
		
		nearHeight = DEFAULT_NEAR_HEIGHT * NEAR_Z * 2;
		nearWidth = ratio * nearHeight;
		
	    // this projection matrix is applied to object coordinates
	    // in the onDrawFrame() method
	    Matrix.frustumM(mProjectionMatrix, 0, -nearWidth / 2, nearWidth / 2, -nearHeight / 2, nearHeight / 2, NEAR_Z, 100);
	}

	/**
	 * Zooms the camera by a given factor
	 *
	 * Scales the size of the near plane by zoomMultiplier and updates the projection matrix.
	 *
	 * @param zoomMultiplier the value by which to scale the near plane
	 */
	public void multiplyZoom(float zoomMultiplier) {
		nearHeight *= zoomMultiplier;
		nearWidth *= zoomMultiplier;

		Matrix.frustumM(mProjectionMatrix, 0, -nearWidth / 2, nearWidth / 2, -nearHeight / 2, nearHeight / 2, NEAR_Z, 100);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig arg1) {
	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	    catchGLError();
	    GLES20.glClearDepthf(1.0f);
	    catchGLError();
	    GLES20.glDepthFunc(GLES20.GL_LEQUAL);
	    catchGLError();
	    GLES20.glDepthMask(true);
	    catchGLError();
	    GLES20.glEnable(GLES20.GL_CULL_FACE);
	    catchGLError();
	    GLES20.glCullFace(GLES20.GL_BACK);
	    catchGLError();
	    GLES20.glEnable(GLES20.GL_BLEND);
	    catchGLError();
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    catchGLError();
	    
		int vertShader = gameView.loadShader(GLES20.GL_VERTEX_SHADER, R.raw.vbasic);
		int fragShader = gameView.loadShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fbasic);
		
		program = GLES20.glCreateProgram();
	    catchGLError();
	    catchGLError();
		GLES20.glAttachShader(program, vertShader);
	    catchGLError();
		GLES20.glAttachShader(program, fragShader);
	    catchGLError();
		GLES20.glLinkProgram(program);
	    catchGLError();

		GLES20.glClearColor(0, 0, 0, 1);
	    catchGLError();
		
		try {
			dungeonManager.currentRoom.load(textureManager);
			load(textureManager);
		    catchGLError();
		} catch (NoFreeTextureUnitsExcpetion e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		lastDrawTime = System.currentTimeMillis();
	}

}
