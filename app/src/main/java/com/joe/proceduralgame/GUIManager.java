package com.joe.proceduralgame;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
	 * Inflates the overlay that shows the beginning of a phase
	 *
	 * @param group the group of the phase
	 */
	public void showPhaseOverlay(final int group) {
		final ViewGroup root = ((ViewGroup) activity.getWindow().getDecorView().getRootView());
		root.getHandler().post(new Runnable() {
			//Thread entry point
			@Override
			public void run() {
				ImageView phaseOverlay = (ImageView) root.findViewById(R.id.phase_overlay);
				if (group == Character.GROUP_PLAYER)
					phaseOverlay.setImageResource(R.drawable.player_phase);
				if (group == Character.GROUP_ENEMY)
					phaseOverlay.setImageResource(R.drawable.enemy_phase);
				phaseOverlay.setVisibility(View.VISIBLE);

				Animation phaseAnimation = AnimationUtils.loadAnimation(activity, R.anim.phase_transition);
				phaseOverlay.startAnimation(phaseAnimation);
			}
		});
	}

	/**
	 * Hides the phase overlay
	 */
	public void hidePhaseOverlay() {
		final ViewGroup root = ((ViewGroup) activity.getWindow().getDecorView().getRootView());
		root.getHandler().post(new Runnable() {
			//Thread entry point
			@Override
			public void run() {
				View phaseOverlay = root.findViewById(R.id.phase_overlay);
				phaseOverlay.setVisibility(View.INVISIBLE);
			}
		});
	}

	/**
	 * Inflates the overlay that shows basic info of a character.
	 *
	 * @param character the character to inspect
	 */
	public void showCharacterSummary(final DungeonManager manager, final Character character) {
		final ViewGroup characterSummary = (ViewGroup) activity.findViewById(R.id.character_summary);

		characterSummary.getHandler().post(new Runnable() {
			//Thread entry point
			@Override
			public void run() {
				synchronized (manager) {
					characterSummary.setVisibility(View.VISIBLE);
					ImageView characterIcon = (ImageView) characterSummary.findViewById(R.id.character_icon);
					characterIcon.setImageResource(character.getIconID());
					HealthBar healthBar = (HealthBar) characterSummary.findViewById(R.id.health_bar);
					healthBar.showHealth(character.getHitPoints(), character.getMaxHitPoints());
					TextView healthText = (TextView) characterSummary.findViewById(R.id.health_text);
					healthText.setText(character.getHitPoints() + " / " + character.getMaxHitPoints());
				}
			}
		});
	}

	/**
	 * Hides the basic character info overlay.
	 */
	public void hideCharacterSummary() {
		final ViewGroup characterSummary = (ViewGroup) activity.findViewById(R.id.character_summary);

		characterSummary.getHandler().post(new Runnable() {
			@Override
			//Thread entry point
			public void run() {
				characterSummary.setVisibility(View.GONE);
			}
		});
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
			//Thread entry point
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
								//Thread entry point
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
	 * Clears the highlight on the currently selected action.
	 */
	public void deselectAction() {
		final ViewGroup actionList = (ViewGroup) activity.findViewById(R.id.action_list);

		actionList.getHandler().post(new Runnable() {
			//Thread entry point
			@Override
			public void run() {
				clearActionHighlights();
			}
		});
	}

	/**
	 * Hides the action pane.
	 */
	public void hideActionPane() {
		final ViewGroup pane = (ViewGroup) activity.findViewById(R.id.action_list);
		if (pane != null) {
			pane.getHandler().post(new Runnable() {
				//Thread entry point
				@Override
				public void run() {
					pane.removeAllViews();
				}
			});
		}
	}

	/**
	 * Removes highlights from all actions in the action pane.
	 */
	private void clearActionHighlights() {
		ViewGroup actionList = (ViewGroup) activity.findViewById(R.id.action_list);
		int nChildren = actionList.getChildCount();
		for (int i = 0; i < nChildren; i++) {
			View entry = actionList.getChildAt(i);
			entry.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	//Thread entry point from click listener
	/**
	 * Called when an action is selected via click in the action pane
	 *
	 * Notifies the controller and clears other
	 *
	 * @param actionEntry the root view of the action entry
	 * @param action the action selected
	 */
	private void selectAction(ViewGroup actionEntry, Action action) {
		if (action == controller.getSelectedAction()) {
			controller.deselectAction();
		} else {
			clearActionHighlights();
			actionEntry.setBackgroundColor(Color.rgb(220, 180, 0));
			controller.onActionSelected(action);
		}
	}

	/**
	 * Displays the given fps measure on screen.
	 *
	 * @param fps the number to display
	 */
	public void displayFPS(final double fps) {
		final ViewGroup actionList = (ViewGroup) activity.findViewById(R.id.action_list);
		final TextView display = (TextView) activity.findViewById(R.id.fps_display);

		Handler handler = actionList.getHandler();
		if (handler != null) {
			handler.post(new Runnable() {
				//Thread entry point
				@Override
				public void run() {
					display.setText(String.valueOf(Math.round(fps)));
				}
			});
		}
	}

	/**
	 * Sets the controller
	 */
	public void setController(Controller controller) {
		this.controller = controller;
	}

}
