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

	public HealthBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float healthDegrees = 315;

		Paint paint = new Paint();
		RectF bounds = new RectF(canvas.getClipBounds());
		paint.setColor(Color.RED);
		canvas.drawArc(bounds, -90 + healthDegrees, 360 - healthDegrees, true, paint);
		paint.setColor(Color.GREEN);
		canvas.drawArc(bounds, -90, healthDegrees, true, paint);
	}

}
