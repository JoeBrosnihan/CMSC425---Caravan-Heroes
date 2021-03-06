package com.joe.proceduralgame;

import android.app.Activity;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

public class GameActivity extends Activity {
	
	private GameGLView glView;
	private boolean started = false;
	private MediaPlayer musicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    setContentView(R.layout.title_screen);

	    musicPlayer = MediaPlayer.create(getApplicationContext(), R.raw.main_theme);
	    musicPlayer.setLooping(true);
    }

	protected void onStart() {
		super.onStart();
		musicPlayer.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		musicPlayer.stop();
	}

	public void onBeginClick(View v) {
		if (started)
			return;
		started = true;

		musicPlayer.stop();

		setContentView(R.layout.activity_game);
		GUIManager guiManager = new GUIManager(this);
		glView = new GameGLView(this, guiManager);
		((ViewGroup) getWindow().getDecorView().getRootView()).addView(glView, 0);

		musicPlayer = MediaPlayer.create(getApplicationContext(), R.raw.dungeon_music_1);
		musicPlayer.setLooping(true);
		musicPlayer.start();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

	/**
	 * Called when the user presses the back button.
	 */
	@Override
	public void onBackPressed() {
		glView.onBackPressed();
	}
    
}
