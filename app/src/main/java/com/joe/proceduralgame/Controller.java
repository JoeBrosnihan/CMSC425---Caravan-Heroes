package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.view.MotionEvent;

import java.util.LinkedList;

public class Controller {
	private GameGLView view;
	private DungeonManager manager;
	private DungeonRenderer renderer;
	private GUIManager gui;
	
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
	 * Called when the user tries to move the selectedCharacter to a square
	 *
	 * @param targetRow the row of the square
	 * @param targetCol the col of the square
	 */
	private void moveToSquare(int targetRow, int targetCol) {
		Room room = manager.currentRoom;
		LinkedList<int[]> path = room.findPath(selectedCharacter.gridRow,
				selectedCharacter.gridCol, targetRow, targetCol, true);
		if (path == null)
			return;
		int moveSquaresLeft = selectedCharacter.getMoveDistance() - selectedCharacter.getSquaresTraversed();
		if (path.size() > moveSquaresLeft)
			return;
		manager.commandMove(selectedCharacter, path);
		gui.hideActionPane();
		renderer.hideMoveOptions();
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

	public boolean onTouchEvent(MotionEvent e) {
		Room room = manager.currentRoom;
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
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
						return true;
					if (manager.getPhaseGroup() != Character.GROUP_PLAYER)
						return true;
					if (selectedCharacter == null) {
						if (targetQuad.type == Type.CHARACTER) {
							Character character = RaycastUtils.quadToCharacter(room, targetQuad);
							if (character.isPlayerOwned()) {
								selectCharacter(character);
								Action[] actions = character.getPossibleActions();
								gui.showActionPane(actions, getActionVisibilities(character, actions));
							}
						}
					} else { //selectedCharacter != null
						if (selectedAction == null) {
							if (targetQuad.type == Type.FLOOR) {
								int targetRow = Math.round(targetQuad.getZ() - room.originz);
								int targetCol = Math.round(targetQuad.getX() - room.originx);
								moveToSquare(targetRow, targetCol);
							} else if (targetQuad.type == Type.CHARACTER) {
								Character character = RaycastUtils.quadToCharacter(room, targetQuad);
								if (character.isPlayerOwned()) {
									selectCharacter(character);
									Action[] actions = character.getPossibleActions();
									gui.showActionPane(actions, getActionVisibilities(character, actions));
								}
							}
						} else { //selectedAction != null
							if (targetQuad.type == Type.CHARACTER || targetQuad.type == Type.NONCHARACTER_ENTITY) {
								if (selectedCharacter.actedThisTurn)
									return true;
								Entity targetEntity = RaycastUtils.quadToEntity(room, targetQuad);
								if (selectedAction.canPerform(selectedCharacter, targetEntity)) {
									manager.commandAction(selectedCharacter, null, selectedAction, targetEntity);
									selectedAction = null;
									gui.hideActionPane();
								}
							}
						}
					}
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

}
