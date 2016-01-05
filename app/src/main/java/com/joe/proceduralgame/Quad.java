package com.joe.proceduralgame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class Quad {
	
	public static enum Type {FLOOR, WALL, DECORATION, CHARACTER, DOOR};

	public float[] modelMatrix;
	public int textureUnit;
	public int textureID;
	public float[] uvOrigin = {0, 0};
	public float[] uvScale = {1, 1};
	public final Type type;
	
	static FloatBuffer vertexBuffer;

	public static final int mBytesPerFloat = 4;
	public static final int mStrideBytes = 5 * mBytesPerFloat;
	public static final int mPositionOffset = 0;
	public static final int mPositionDataSize = 3;
	public static final int mTexCoordsOffset = 3;
	public static final int mTexCoordsDataSize = 2;
	
	static float squareCoords[] = {
			-0.5f, 0.5f, 0.0f, // top left
			0, 0,
			0.5f, 0.5f, 0.0f, // top right
			1, 0,
			0.5f, -0.5f, 0.0f, // bottom right
			1, 1,
			-0.5f, -0.5f, 0.0f, // bottom left
			0, 1};
	
	public static Quad createStaticQuad(Type type, float[] modelMatrix, int textureID) {
		Quad q = new Quad(type, modelMatrix);
		q.textureID = textureID;
		return q;
	}
	
	public static Quad createDynamicQuad(Type type, float[] modelMatrix, int textureUnit) {
		Quad q = new Quad(type, modelMatrix);
		q.textureUnit = textureUnit;
		return q;
	}
	
	private Quad(Type type, float[] modelMatrix) {
		this.type = type;
		this.modelMatrix = modelMatrix.clone();
		loadBuffer();
	}

	public static void loadBuffer() {
		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(
		// (# of coordinate values * 4 bytes per float)
				squareCoords.length * 4);
		bb.order(ByteOrder.nativeOrder());
		vertexBuffer = bb.asFloatBuffer();
		vertexBuffer.put(squareCoords);
		vertexBuffer.position(0);
	}
	
	public static void enableArrays(int shaderProgram) {
		int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
		int texCoordsHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoords");
		
		vertexBuffer.position(mPositionOffset);
		GLES20.glVertexAttribPointer(positionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, mStrideBytes, vertexBuffer);
		GLES20.glEnableVertexAttribArray(positionHandle);
		
		vertexBuffer.position(mTexCoordsOffset);
		GLES20.glVertexAttribPointer(texCoordsHandle, mTexCoordsDataSize, GLES20.GL_FLOAT, false, mStrideBytes, vertexBuffer);
		GLES20.glEnableVertexAttribArray(texCoordsHandle);
	}
	
	public static void disableArrays(int shaderProgram) {
		int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
		int texCoordsHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoords");
		
		GLES20.glDisableVertexAttribArray(positionHandle);
		GLES20.glDisableVertexAttribArray(texCoordsHandle);
	}

	public void draw(int shaderProgram, float[] mVPMatrix) {
		float[] mvp = new float[16];
		Matrix.multiplyMM(mvp, 0, mVPMatrix, 0, modelMatrix, 0);

		int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
		int texCoordsHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoords");
		
		vertexBuffer.position(mPositionOffset);
		GLES20.glVertexAttribPointer(positionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, mStrideBytes, vertexBuffer);
		GLES20.glEnableVertexAttribArray(positionHandle);
		
		vertexBuffer.position(mTexCoordsOffset);
		GLES20.glVertexAttribPointer(texCoordsHandle, mTexCoordsDataSize, GLES20.GL_FLOAT, false, mStrideBytes, vertexBuffer);
		GLES20.glEnableVertexAttribArray(texCoordsHandle);
		
		int mvpHandle = GLES20.glGetUniformLocation(shaderProgram, "MVP");
		GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
		
//		int modelHandle = GLES20.glGetUniformLocation(shaderProgram, "modelMatrix");
//		GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0);

		int textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
		GLES20.glUniform1i(textureHandle, textureUnit);
		
		int uvOriginHandle = GLES20.glGetUniformLocation(shaderProgram, "uvOrigin");
		int uvScaleHandle = GLES20.glGetUniformLocation(shaderProgram, "uvScale");
		GLES20.glUniform2f(uvOriginHandle, uvOrigin[0], uvOrigin[1]);
		GLES20.glUniform2f(uvScaleHandle, uvScale[0], uvScale[1]);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		
		GLES20.glDisableVertexAttribArray(positionHandle);
		GLES20.glDisableVertexAttribArray(texCoordsHandle);
		
	}

	public void destroy() {

	}
	
	public float getX() {
		return modelMatrix[12];
	}
	
	public float getY() {
		return modelMatrix[13];
	}
	
	public float getZ() {
		return modelMatrix[14];
	}

}
