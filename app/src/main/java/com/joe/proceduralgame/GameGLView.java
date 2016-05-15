package com.joe.proceduralgame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class GameGLView extends GLSurfaceView {
	
	private final TextureManager textureManager;
	private GUIManager guiManager;
	private DungeonRenderer gameRenderer;
	private DungeonManager dungeonManager;
	private Controller gameController;
	
	public GameGLView(Context context, GUIManager guiManager) {
		super(context);
		this.guiManager = guiManager;
		setEGLContextClientVersion(2);
		
		textureManager = new TextureManager(context);
		dungeonManager = new DungeonManager();
		dungeonManager.setGUIManager(guiManager);
		
		gameRenderer = new DungeonRenderer(this, dungeonManager, textureManager);
		dungeonManager.setDungeonRenderer(gameRenderer);
		setRenderer(gameRenderer);

		gameController = new Controller(this, gameRenderer, dungeonManager, guiManager);
		guiManager.setController(gameController);
		dungeonManager.setController(gameController);
		gameRenderer.gameController = gameController;

		dungeonManager.start();
	}

	//Thread entry point
	public boolean onTouchEvent(MotionEvent e) {
		synchronized (dungeonManager) {
			return gameController.onTouchEvent(e);
		}
	}

	/**
	 * Called when the user presses the back button.
	 */
	public void onBackPressed() {
		synchronized (dungeonManager) {
			gameController.onBackPressed();
		}
	}
	
	public int loadShader(int type, int resID) {
		String shaderCode = readTxt(resID);
		System.out.println("reading shader file:\n" + shaderCode);
		return loadShader(type, shaderCode);
	}

	public int loadShader(int type, String shaderCode) {
		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
		// or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
		int shader = GLES20.glCreateShader(type);

		// add the source code to the shader and compile it
		GLES20.glShaderSource(shader, shaderCode);
		int e = GLES20.glGetError();
		GLES20.glCompileShader(shader);
		e = GLES20.glGetError();

		return shader;
	}
	
	private String readTxt(int resID) {
		InputStream inputStream = getResources().openRawResource(resID);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int i;
		try {
			i = inputStream.read();
			while (i != -1) {
				byteArrayOutputStream.write(i);
				i = inputStream.read();
			}
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return byteArrayOutputStream.toString();
	}

}
