package com.joe.proceduralgame;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.joe.proceduralgame.Quad.Type;
import com.joe.proceduralgame.TextureManager.NoFreeTextureUnitsExcpetion;

public class DungeonRenderer implements GLSurfaceView.Renderer {
	
	public static final int COORDS_PER_VERTEX = 2;
	public static final float NEAR_PLANE = 1;
	public static final float CAMERA_ANGLE = 60;
	
	private GameGLView gameView;
	private TextureManager textureManager;
	Controller gameController;
	
	private int program;
	private DungeonManager dungeonManager;
	private FloatBuffer vertexBuffer;
	private final float[] mVPMatrix = new float[16];
	private final float[] mProjectionMatrix = new float[16];
	final float[] mViewMatrix = new float[16];
	private float fov = 45;
	public float nearWidth, nearHeight;
	float camx, camy, camz;
	
	private Quad characterSelector;
	
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
    	
	    GLES20.glDisable(GLES20.GL_CULL_FACE);
	    for (EdgeEntity e : dungeonManager.currentRoom.edgeEntities) {
	    	e.draw(program, mVPMatrix);
	    }
	    for (Entity e : dungeonManager.currentRoom.entities) {
	    	e.draw(program, mVPMatrix);
	    }
		for (DamageDisplay d : dungeonManager.currentRoom.damageDisplays) {
			d.draw(program, mVPMatrix);
		}
	}

	public void onDrawFrame(GL10 unused) {
		double t = System.currentTimeMillis() / 1000.0;
		
		// Set the camera position (View matrix)
		camy = 8;
		camx = dungeonManager.leader.posx;
		camz = dungeonManager.leader.posz;
	    
	    Matrix.setIdentityM(mViewMatrix, 0);
	    Matrix.translateM(mViewMatrix, 0, 0, 0, -camy);
	    Matrix.rotateM(mViewMatrix, 0, CAMERA_ANGLE, 1, 0, 0);
	    Matrix.translateM(mViewMatrix, 0, -camx, 0, -camz);

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
		
	    catchGLError();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    catchGLError();
		
		draw();
	    catchGLError();
		
	}
	
	private void load(TextureManager tex) throws NoFreeTextureUnitsExcpetion {
		Quad.loadBuffer();
		
		int selectorTex;
		selectorTex = tex.referenceLoad(R.drawable.ui_atlas_1);
		characterSelector = Quad.createDynamicQuad(Type.DECORATION, new float[16], selectorTex);
		characterSelector.uvOrigin[0] = 3f / 8f;
		characterSelector.uvOrigin[1] = 0f;
		characterSelector.uvScale[0] = 1f / 8f;
		characterSelector.uvScale[1] = 1f / 8f;
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		float ratio = width / (float) height;
		float h = (float) Math.tan(Math.toRadians(fov / 2));
		
		nearHeight = h * NEAR_PLANE * 2;
		nearWidth = ratio * nearHeight;
		
	    // this projection matrix is applied to object coordinates
	    // in the onDrawFrame() method
	    Matrix.frustumM(mProjectionMatrix, 0, -nearWidth / 2, nearWidth / 2, -nearHeight / 2, nearHeight / 2, NEAR_PLANE, 100);
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
		
	}

}
