package com.joe.proceduralgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Admin on 3/11/2016.
 */
public class HealthBar extends View {

	private int health, maxHealth;

	public HealthBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		maxHealth = 1;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float healthDegrees = 360.0f * health / maxHealth;

		Paint paint = new Paint();
		RectF bounds = new RectF(canvas.getClipBounds());
		paint.setColor(Color.rgb(225, 0, 0));
		canvas.drawArc(bounds, -90 + healthDegrees, 360 - healthDegrees, true, paint);
		paint.setColor(Color.rgb(0, 225, 0));
		canvas.drawArc(bounds, -90, healthDegrees, true, paint);
	}

	public void showHealth(int health, int maxHealth) {
		this.health = health;
		this.maxHealth = maxHealth;
		invalidate();
	}

}
