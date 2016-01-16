package com.joe.proceduralgame;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.ViewGroup;

public class GameActivity extends Activity {
	
	private GLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    setContentView(R.layout.activity_game);
	    GUIManager guiManager = new GUIManager(this);
        glView = new GameGLView(this, guiManager);
	    ((ViewGroup) getWindow().getDecorView().getRootView()).addView(glView, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }
    
}
