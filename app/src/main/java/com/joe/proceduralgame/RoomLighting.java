package com.joe.proceduralgame;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admin on 4/27/2016.
 */
public class RoomLighting {

	public static int MAX_LIGHTS = 16;

	private List<Light> lights = new ArrayList<>();
	private boolean needsUpdate = false;

	public class Light {
		private float x, y, z, r, g, b;
	}

	/**
	 * Adds a light to the room's lighting.
	 *
	 * @param light the light to add
	 */
	public void addLight(Light light) {
		lights.add(light);
		needsUpdate = true;
	}

	/**
	 * Checks if the GL uniforms need to be updated to match changes in lighting
	 *
	 * @return true if lighting has changed since last update
	 */
	public boolean needsUpdate() {
		return needsUpdate;
	}

	/**
	 * Updates the GL uniforms to match the current lighting.
	 *
	 * @param program the GL handle for the active shader program
	 */
	public void update(int program) {
		//TODO this only supports adding new static lights at the moment
		int visibleLights = Math.min(lights.size(), MAX_LIGHTS);

		int lightPosHandle = GLES20.glGetUniformLocation(program, "lightPos");
		int lightColorHandle = GLES20.glGetUniformLocation(program, "lightColor");
		int nLightsHandle = GLES20.glGetUniformLocation(program, "nLights");
		GLES20.glUniform1i(nLightsHandle, visibleLights);

		for (int i = 0; i < visibleLights; i++) {
			Light light = lights.get(i);
			GLES20.glUniform3f(lightPosHandle + i, light.x, light.y, light.z);
			GLES20.glUniform3f(lightColorHandle + i, light.r, light.g, light.b);
		}
		needsUpdate = false;
	}

	/**
	 * Creates a new light
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param r
	 * @param g
	 * @param b
	 * @return the created light
	 */
	public Light createLight(float x, float y, float z, float r, float g, float b) {
		Light light = new Light();
		light.x = x;
		light.y = y;
		light.z = z;
		light.r = r;
		light.g = g;
		light.b = b;
		return light;
	}

}
