package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Controller {
	public static final float MIN_DIST_TO_START_PANNING = .5f;

	private GameGLView view;
	private DungeonManager manager;
	private DungeonRenderer renderer;
	private GUIManager gui;

	/** The world {x, z} world coordinates of where the touch started */
	private float[] touchAnchor = new float[2];
	private float[] touchPos = new float[2];
	private double pixelDist; //The current pixel distance between two touches when zooming
	private boolean panning = false;

	public Character selectedCharacter = null;
	private Action selectedAction = null;

	public Controller(GameGLView view, DungeonRenderer renderer, DungeonManager manager, GUIManager guiManager) {
		this.view = view;
		this.renderer = renderer;
		this.manager = manager;
		this.gui = guiManager;
	}

	private void selectCharacter(Character character) {
		selectedCharacter = character;
		renderer.setFocus(character);
		int movesLeft = character.getMoveDistance() - character.getSquaresTraversed();
		int[][] moveOptions = manager.currentRoom.getRange(character.gridRow, character.gridCol, movesLeft);
		renderer.showMoveOptions(moveOptions);
	}

	/**
	 * Handles when the user purposefully deselects a character.
	 */
	private void deselectCharacter() {
		selectedCharacter = null;
		renderer.setFocus(null);
		renderer.hideMoveOptions();
		gui.hideActionPane();
		gui.hideCharacterSummary();
	}

	/**
	 * Handles when the user purposefully deselects their selected action.
	 */
	public void deselectAction() {
		selectedAction = null;
		gui.deselectAction();
		renderer.hideAttackOptions();
	}

	/**
	 * Handles when the user tries to move the selectedCharacter to a square.
	 * selectedCharacter must a Character in the currentRoom.
	 *
	 * @param targetRow the row of the square
	 * @param targetCol the col of the square
	 */
	private void moveToSquare(int targetRow, int targetCol) {
		Room room = manager.currentRoom;
		LinkedList<int[]> path = room.findPath(selectedCharacter.gridRow,
				selectedCharacter.gridCol, targetRow, targetCol, true);
		int moveSquaresLeft = selectedCharacter.getMoveDistance() - selectedCharacter.getSquaresTraversed();
		if (path == null || path.size() > moveSquaresLeft) {
			deselectCharacter();
		} else {
			manager.commandMove(selectedCharacter, path);
			gui.hideActionPane();
			renderer.hideMoveOptions();
		}
	}

	/**
	 * Called by the DungeonManager when the room becomes tranquil.
	 */
	public void onBecomeTranquil() {
		if (manager.getPhaseGroup() != Character.GROUP_PLAYER) {
			selectedCharacter = null;
		} else if (manager.getPhaseGroup() == Character.GROUP_PLAYER && selectedCharacter != null) {
			if (!selectedCharacter.actedThisTurn) {
				int movesLeft = selectedCharacter.getMoveDistance() - selectedCharacter.getSquaresTraversed();
				int[][] moveOptions = manager.currentRoom.getRange(selectedCharacter.gridRow, selectedCharacter.gridCol, movesLeft);
				renderer.showMoveOptions(moveOptions);
				Action[] actions = selectedCharacter.getPossibleActions();
				gui.showActionPane(actions, getActionVisibilities(selectedCharacter, actions));
			}
		}
	}

	/**
	 * Called by the GUIManager when an action is selected from the gui
	 * @param selectedAction the selected Action
	 */
	public void onActionSelected(Action selectedAction) {
		this.selectedAction = selectedAction;

		if (selectedAction.singleTarget) {
			if (selectedAction == Action.basicAttack) {
				//TODO display range?
				List<Entity> targets = selectedAction.getTargets(selectedCharacter);
				renderer.showAttackOptions(targets);
			}
		}
	}

	/**
	 * Called when the user releases a touch that was held in one place (without panning).
	 *
	 * @param e the MotionEvent that triggered this
	 */
	private void onClick(MotionEvent e) {
		Room room = manager.currentRoom;
		float nearX = (e.getX() / view.getWidth() - .5f) * renderer.nearWidth;
		float nearY = (-e.getY() / view.getHeight() + .5f) * renderer.nearHeight;

		Quad targetQuad = RaycastUtils.pick(room, renderer.mViewMatrix, nearX, nearY);
		if (targetQuad != null) {

			if (manager.neutral) {
				if (targetQuad.type == Type.CHARACTER || targetQuad.type == Type.NONCHARACTER_ENTITY) {
					if (selectedCharacter == null) {
						if (targetQuad.type == Type.CHARACTER) {
							Character character = RaycastUtils.quadToCharacter(room, targetQuad);
							if (character.isPlayerOwned())
								selectCharacter(character);
						}
					} else {
						Entity targetEntity = RaycastUtils.quadToEntity(room, targetQuad);
						Action defaultAction = targetEntity.getDefaultAction();
						if (defaultAction != null) {
							if (defaultAction.canPerform(selectedCharacter, targetEntity)) {
								//get square
								LinkedList<int[]> path = room.findPath(selectedCharacter.gridRow,
										selectedCharacter.gridCol, targetEntity.gridRow,
										targetEntity.gridCol, false);
								if (path != null) { //if there is a free square
									manager.commandAction(selectedCharacter, path,
											Action.basicAttack, targetEntity);
								}
							}
						}
					}
				} else if (targetQuad.type == Type.FLOOR) {
					if (selectedCharacter != null) {
						int targetRow = (int) Math.round(targetQuad.getZ() - room.originz);
						int targetCol = (int) Math.round(targetQuad.getX() - room.originx);
						LinkedList<int[]> path = room.findPath(selectedCharacter.gridRow,
								selectedCharacter.gridCol, targetRow, targetCol, true);
						if (path != null) {
							manager.commandMove(selectedCharacter, path);
						}
					}
				}


			} else { //manager.neutral == false
				if (!manager.isTranquil())
					return;
				if (manager.getPhaseGroup() != Character.GROUP_PLAYER)
					return;
				if (selectedCharacter == null) {
					if (targetQuad.type == Type.CHARACTER) {
						Character character = RaycastUtils.quadToCharacter(room, targetQuad);
						if (character.isPlayerOwned()) {
							selectCharacter(character);
							//TODO should all of the following be moved to within selectCharacter()?
							gui.showCharacterSummary(character);
							Action[] actions = character.getPossibleActions();
							gui.showActionPane(actions, getActionVisibilities(character, actions));
						}
					}
				} else { //selectedCharacter is not null
					if (selectedAction == null) {
						if (targetQuad.type == Type.FLOOR) {
							int targetRow = Math.round(targetQuad.getZ() - room.originz);
							int targetCol = Math.round(targetQuad.getX() - room.originx);
							moveToSquare(targetRow, targetCol);
						} else if (targetQuad.type == Type.CHARACTER) {
							Character character = RaycastUtils.quadToCharacter(room, targetQuad);
							if (character.isPlayerOwned()) {
								selectCharacter(character);
								//TODO should all of the following be moved to within selectCharacter()?
								gui.showCharacterSummary(character);
								Action[] actions = character.getPossibleActions();
								gui.showActionPane(actions, getActionVisibilities(character, actions));
							}
						}
					} else { //selectedAction != null
						if (targetQuad.type == Type.CHARACTER || targetQuad.type == Type.NONCHARACTER_ENTITY) {
							if (selectedCharacter.actedThisTurn)
								return;
							Entity targetEntity = RaycastUtils.quadToEntity(room, targetQuad);
							if (selectedAction.canPerform(selectedCharacter, targetEntity)) {
								manager.commandAction(selectedCharacter, null, selectedAction, targetEntity);
								selectedAction = null;
								gui.hideActionPane();
								gui.hideCharacterSummary();
								renderer.hideMoveOptions();
							}
							deselectAction();
						} else {
							deselectAction();
						}
					}
				}
			}
		}
	}

	/**
	 * Called when the user presses the back button.
	 */
	public void onBackPressed() {
		if (manager.neutral) {
		} else { //manager.neutral is false
			if (selectedCharacter == null) {
			} else { //selectedCharacter is not null
				if (selectedAction == null) {
					deselectCharacter();
				} else { //selectedAction is not null
					deselectAction();
				}
			}
		}
	}

	/**
	 * Handles touch events
	 *
	 * @param e the MotionEvent to handle
	 * @return true if the event was consumed
	 */
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getActionMasked() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
			if (e.getPointerCount() > 1) {
				renderer.setFocus(null);
				panning = true;
				pixelDist = Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1)); // this may produce weird behavior for > 2 touches because of hardcoded indices
			}

			float avgX = 0, avgY = 0;
			for (int i = 0; i < e.getPointerCount(); i++) {
				avgX += e.getX(i);
				avgY += e.getY(i);
			}
			avgX /= e.getPointerCount();
			avgY /= e.getPointerCount();
			project(touchAnchor, avgX, avgY);

		} else if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			int index = e.getActionIndex();

			float avgX = 0, avgY = 0;
			for (int i = 0; i < e.getPointerCount(); i++) {
				if (i != index) {
					avgX += e.getX(i);
					avgY += e.getY(i);
				}
			}
			avgX /= (e.getPointerCount() - 1);
			avgY /= (e.getPointerCount() - 1);
			project(touchAnchor, avgX, avgY);

			if (e.getPointerCount() == 1) {
				if (!panning)
					onClick(e);
				else
					panning = false;
			}
		} else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
			if (!panning) {
				project(touchPos, e.getX(), e.getY());
				double dist = Math.hypot(touchPos[0] - touchAnchor[0], touchPos[1] - touchAnchor[1]);
				if (dist >= MIN_DIST_TO_START_PANNING) {
					renderer.setFocus(null);
					panning = true;
				}
			}
			if (e.getPointerCount() > 1) {
				double dist = Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
				renderer.multiplyZoom((float) (pixelDist / dist));
				pixelDist = dist;
			}
			if (panning) {
				float avgX = 0, avgY = 0;
				for (int i = 0; i < e.getPointerCount(); i++) {
					avgX += e.getX(i);
					avgY += e.getY(i);
				}
				avgX /= e.getPointerCount();
				avgY /= e.getPointerCount();
				project(touchPos, avgX, avgY);

				float dx = touchAnchor[0] - touchPos[0];
				float dz = touchAnchor[1] - touchPos[1];
				renderer.destx = renderer.camx + dx;
				renderer.destz = renderer.camz + dz;

				//confine camera to current room
				final float left = manager.currentRoom.originx,
						right = manager.currentRoom.originx + manager.currentRoom.width - 1,
						near = manager.currentRoom.originz + manager.currentRoom.length - 1,
						far = manager.currentRoom.originz;
				if (renderer.destx < left) {
					renderer.destx = left;
					if (renderer.camx < left + .1f && dx < 0)
						touchAnchor[0] = touchPos[0];
				} else if (renderer.destx > right) {
					renderer.destx = right;
					if (renderer.camx > right - .1f && dx > 0)
						touchAnchor[0] = touchPos[0];
				}
				if (renderer.destz < far) {
					renderer.destz = far;
					if (renderer.camz < far + .1f && dz < 0)
						touchAnchor[1] = touchPos[1];
				} else if (renderer.destz > near) {
					renderer.destz = near;
					if (renderer.camz > near - .1f && dz > 0)
						touchAnchor[1] = touchPos[1];
				}
			}
		}
		return true;
	}

	/**
	 * Projects a set of on-screen pixel coordinates onto the ground
	 *
	 * @param result float[] to store the resulting {x, y}
	 * @param x the on-screen x coordinate to project
	 * @param y the on-screen y coordinate to project
	 */
	private final void project(float[] result, float x, float y) {
		float nearX = (x / view.getWidth() - .5f) * renderer.nearWidth;
		float nearY = (-y / view.getHeight() + .5f) * renderer.nearHeight;
		RaycastUtils.projectOntoGround(result, renderer.mViewMatrix, nearX, nearY);
	}

	/**
	 * Gets the visibilities of an array of actions
	 * @param actor the character to perform an action
	 * @param actions the actions to check
	 * @return an array of the visibility corresponding to each action
	 */
	private Action.Visibility[] getActionVisibilities(Character actor, Action[] actions) {
		Action.Visibility[] visibilities = new Action.Visibility[actions.length];
		for (int i = 0; i < actions.length; i++) {
			visibilities[i] = actions[i].getVisibility(actor);
		}
		return visibilities;
	}

	/**
	 * Gets the selected Action.
	 *
	 * @return the selectedAction
	 */
	public Action getSelectedAction() {
		return selectedAction;
	}

}
