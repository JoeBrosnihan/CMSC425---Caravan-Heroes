package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.util.Log;
import android.view.MotionEvent;

import java.util.LinkedList;

public class Controller {
	public static final float MIN_DIST_TO_START_PANNING = .5f;

	private GameGLView view;
	private DungeonManager manager;
	private DungeonRenderer renderer;
	private GUIManager gui;

	/** The world {x, z} world coordinates of where the touch started */
	private float[] touchAnchor = new float[2];
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

	private float[] touchPos = new float[2];
	public boolean onTouchEvent(MotionEvent e) {
		float nearX = (e.getX() / view.getWidth() - .5f) * renderer.nearWidth;
		float nearY = (-e.getY() / view.getHeight() + .5f) * renderer.nearHeight;

		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			RaycastUtils.projectOntoGround(touchAnchor, renderer.mViewMatrix, nearX, nearY);
		} else if (e.getAction() == MotionEvent.ACTION_UP) {
			if (panning)
				panning = false;
			else
				onClick(e);
		} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			RaycastUtils.projectOntoGround(touchPos, renderer.mViewMatrix, nearX, nearY);
			if (!panning) {
				if (touchAnchor == null) {
					touchAnchor = touchPos;
				} else {
					double dist = Math.hypot(touchPos[0] - touchAnchor[0], touchPos[1] - touchAnchor[1]);
					if (dist >= MIN_DIST_TO_START_PANNING) {
						renderer.setFocus(null);
						panning = true;
					}
				}
			}
			if (panning) {
				//Calculate vector that should be added to camera's position
				float dx = touchAnchor[0] - touchPos[0];
				float dz = touchAnchor[1] - touchPos[1];
				renderer.destx = renderer.camx + dx;
				renderer.destz = renderer.camz + dz;
				//Confine camera to current room
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
