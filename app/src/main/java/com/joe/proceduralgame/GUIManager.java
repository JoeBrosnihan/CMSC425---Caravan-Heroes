package com.joe.proceduralgame;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
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

    private Activity activity;
	private Controller controller;

    public GUIManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * (Re)Inflates the action pane and populates it with entries (icons and labels) for the given
     * actions.
     * @param actions an array of Actions to display
     * @param visibilities an array of the corresponding visibilities of each Action
     */
    public void showActionPane(final Action[] actions, final Action.Visibility[] visibilities) {
	    final ViewGroup actionList = (ViewGroup) activity.findViewById(R.id.action_list);

	    actionList.getHandler().post(new Runnable() {
		    @Override
		    public void run() {
			    actionList.removeAllViews(); //clear children in case it
			    LayoutInflater inflater = activity.getLayoutInflater();
			    //TODO need to synchronize on the game thread if checking actions
			    //or maybe I should design somehow else to avoid this complexity
			    for (int i = 0; i < actions.length; i++) {
				    if (visibilities[i] != Action.Visibility.HIDDEN) {
					    final int index = i;
					    final ViewGroup entry = (ViewGroup) inflater.inflate(R.layout.layout_action_entry, null);
					    if (visibilities[index] == Action.Visibility.NOT_SELECTABLE) {
						    ((TextView) entry.findViewById(R.id.action_label)).setTextColor(Color.GRAY);
						    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
							    entry.findViewById(R.id.action_icon).setAlpha(.7f);
						    }
					    } else if (visibilities[index] == Action.Visibility.SELECTABLE) {
						    entry.setOnClickListener(new View.OnClickListener() {
							    @Override
							    public void onClick(View v) {
								    selectAction(entry, actions[index]);
							    }
						    });
					    }
					    actionList.addView(entry);
					    ((TextView) entry.findViewById(R.id.action_label)).setText(actions[i].name);
					    ((ImageView) entry.findViewById(R.id.action_icon)).setImageResource(actions[i].icon_id);
				    }
			    }
		    }
	    });
    }

	/**
	 * Hides the action pane.
	 */
	public void hideActionPane() {
		ViewGroup pane = (ViewGroup) activity.findViewById(R.id.action_list);
		if (pane != null)
			pane.removeAllViews();
	}

	/**
	 * Called when an action is selected via click in the action pane
	 *
	 * Notifies the controller and clears other
	 *
	 * @param actionEntry the root view of the action entry
	 * @param action the action selected
	 */
	private void selectAction(ViewGroup actionEntry, Action action) {
		//TODO clear others

		actionEntry.setBackgroundColor(Color.rgb(220, 180, 0));
		controller.onActionSelected(action);
	}

	/**
	 * Sets the controller
	 */
	public void setController(Controller controller) {
		this.controller = controller;
	}

}