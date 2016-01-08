package com.joe.proceduralgame;

import android.opengl.Matrix;

/**
 * Represents numbers that pop up above character heads to signify a change in health
 */
public class DamageDisplay {

    private static float SCALE_X = .5f, SCALE_Y = .5f, INITIAL_Y = 1.f, SPEED_Y = .5f;
    //How long the display will last before disappearing in ms
    public static final long LIFETIME = 500;

    public long creationTime;
    //In order left to right
    private Quad[] digits;
    private float posx, posz;
    private float[] baseMatrix = new float[16];
    private int ndigits;

    /**
     * Creates a display for the given damage amount.
     *
     * @param amount the amount of damage taken
     * @param textureUnit the texture unit of the atlas containing damage numbers
     */
    public DamageDisplay(int amount, int textureUnit, float posx, float posz) {
        this.posx = posx;
        this.posz = posz;
        creationTime = System.currentTimeMillis();

        Matrix.setIdentityM(baseMatrix, 0);
        Matrix.translateM(baseMatrix, 0, posx, 0, posz);
        Matrix.rotateM(baseMatrix, 0, Character.TILT_ANGLE, -1, 0, 0);
        Matrix.translateM(baseMatrix, 0, 0, INITIAL_Y, 0);
        Matrix.scaleM(baseMatrix, 0, SCALE_X, SCALE_Y, 1);

        ndigits = 0;
        int partialAmount = amount;
        while (partialAmount > 0) {
            ndigits++;
            partialAmount /= 10;
        }
        digits = new Quad[ndigits];
        partialAmount = amount;
        for (int i = 0; i < ndigits; i++) {
            //TODO tile multiple digits right to left
            digits[ndigits - i - 1] = Quad.createDynamicQuad(Quad.Type.DECORATION, new float[16], textureUnit);
            setQuadDigit(digits[ndigits - i - 1], partialAmount % 10);
            partialAmount /= 10;
        }
    }

    /**
     * Draws the numbers.
     *
     * @param shaderProgram the int handle on the shader program for rendering
     * @param mVPMatrix the View Projection matrix of the viewer
     * @param uiTextureUnit the int unit of the atlas containing the digit textures
     */
    public void draw(int shaderProgram, float[] mVPMatrix, int uiTextureUnit) {
        float offx = -(ndigits - 1) / 2.0f;
        float offy = (System.currentTimeMillis() - creationTime) * .001f * SPEED_Y;
        for (int i = 0; i < digits.length; i++) {
            digits[i].textureUnit = uiTextureUnit;
            Matrix.translateM(digits[i].modelMatrix, 0, baseMatrix, 0, (offx + i) * SCALE_X, offy, 0);
            digits[i].draw(shaderProgram, mVPMatrix);
        }
    }

    /**
     * Sets the UV coordinates of the quad to the given digit.
     */
    private void setQuadDigit(Quad quad, int digit) {
        int nCol = 8;
        int index;
        switch (digit) {
        case 0:
            index = 18;
            break;
        case 1:
            index = 8;
            break;
        case 2:
            index = 9;
            break;
        case 3:
            index = 10;
            break;
        case 4:
            index = 11;
            break;
        case 5:
            index = 12;
            break;
        case 6:
            index = 13;
            break;
        case 7:
            index = 14;
            break;
        case 8:
            index = 16;
            break;
        case 9:
            index = 17;
            break;
        default:
            return; //should never happen
        }
        quad.uvOrigin[0] = (index % nCol) / (float) nCol; // u coord
        quad.uvOrigin[1] = (index / nCol) / (float) nCol; // v coord
        quad.uvScale[0] = 1 / (float) nCol; // u scale
        quad.uvScale[1] = 1 / (float) nCol; // v scale
    }

}
