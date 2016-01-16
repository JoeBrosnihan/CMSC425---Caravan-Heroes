package com.joe.proceduralgame;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Provides an interface for the Controller and DungeonManager to modify the GUI
 *
 * The GUI is a combination of Android Views, as opposed to rendered in OpenGL.
 *
 * This class should not need access to much information. All information necessary will be passed
 * in method parameters. This class should only know about individual game Objects. The Controller
 * and DungeonManager will determine what methods to call appropriately. The context will be
 * handled in those classes, not here.
 */
public class GUIManager {

    Activity activity;

    public GUIManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * (Re)Inflates the action pane and populates it with entries (icons and labels) for the given
     * actions.
     * @param actions an array of Actions to display
     */
    public void showActionPane(Action[] actions) {
	    ViewGroup actionList = (ViewGroup) activity.findViewById(R.id.action_list);
	    actionList.removeAllViews(); //clear children in case it
	    LayoutInflater inflater = activity.getLayoutInflater();
	    //TODO need to synchronize on the game thread if checking actions
	        //or maybe I should design somehow else to avoid this complexity
	    for (Action a : actions) {
		    ViewGroup entry = (ViewGroup) inflater.inflate(R.layout.layout_action_entry, null);
		    actionList.addView(entry);
		    ((TextView) entry.findViewById(R.id.action_label)).setText(a.name);
		    ((ImageView) entry.findViewById(R.id.action_icon)).setImageResource(R.drawable.attack_action_icon);
	    }
    }

	/**
	 * Hides the action pane.
	 */
	public void hideActionPane() {
		((ViewGroup) activity.findViewById(R.id.action_list)).removeAllViews();
	}

}
