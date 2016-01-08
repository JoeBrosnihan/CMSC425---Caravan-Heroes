package com.joe.proceduralgame;

import android.opengl.Matrix;

/**
 * Represents numbers that pop up above character heads to signify a change in health
 */
public class DamageDisplay {

    private static float SCALE_X = 1, SCALE_Y = 1, OFFSET_Z = .1f, INITIAL_Y = 1.f;

    private Quad[] digits;
    private float posx, posz;

    /**
     * Creates a display for the given damage amount.
     *
     * @param amount the amount of damage taken
     * @param textureUnit the texture unit of the atlas containing damage numbers
     */
    public DamageDisplay(int amount, int textureUnit, float posx, float posz) {
        this.posx = posx;
        this.posz = posz;
        int ndigits = 0;
        int partialAmount = amount;
        while (partialAmount > 0) {
            ndigits++;
            partialAmount /= 10;
        }
        digits = new Quad[ndigits];
        partialAmount = amount;
        for (int i = 0; i < ndigits; i++) {
            //construct model matrix
            float[] m = new float[16];
            //TODO tile multiple digits right to left
            Matrix.setIdentityM(m, 0);
            Matrix.translateM(m, 0, posx, 0, posz);
            Matrix.rotateM(m, 0, Character.TILT_ANGLE, -1, 0, 0);
            Matrix.translateM(m, 0, 0, INITIAL_Y, OFFSET_Z);
            Matrix.scaleM(m, 0, SCALE_X, SCALE_Y, 1);
            digits[i] = Quad.createDynamicQuad(Quad.Type.DECORATION, m, textureUnit);
            setQuadDigit(digits[i], partialAmount % 10);
            partialAmount /= 10;
        }
    }

    public void draw(int shaderProgram, float[] mVPMatrix) {
        //TODO synchronize if the model matrix ever gets updated (maybe?)
        for (int i = 0; i < digits.length; i++) {
            digits[i].draw(shaderProgram, mVPMatrix);
        }
    }

    /**
     * Sets the UV coordinates of the quad to the given digit.
     */
    private void setQuadDigit(Quad quad, int digit) {
        //TODO implement
    }

}
