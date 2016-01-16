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

	public Controller(GameGLView view, DungeonRenderer renderer, DungeonManager manager, GUIManager guiManager) {
		this.view = view;
		this.renderer = renderer;
		this.manager = manager;
		this.gui = guiManager;
	}

	private void selectCharacter(Character character) {
		selectedCharacter = character;
		renderer.setFocus(character);
	}

	/**
	 * Called by the DungeonManager when the room becomes tranquil.
	 */
	public void onBecomeTranquil() {
		if (manager.getPhaseGroup() == Character.GROUP_PLAYER && selectedCharacter != null) {
			if (!selectedCharacter.actedThisTurn) {
				gui.showActionPane(selectedCharacter.getPossibleActions());
			}
		}
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
					if (manager.getPhaseGroup() != Character.GROUP_PLAYER)
						return true;
					if (selectedCharacter == null) {
						if (targetQuad.type == Type.CHARACTER) {
							Character character = RaycastUtils.quadToCharacter(room, targetQuad);
							if (character.isPlayerOwned()) {
								selectCharacter(character);
								gui.showActionPane(character.getPossibleActions());
							}
						}
					} else { //selectedCharacter != null
						if (targetQuad.type == Type.FLOOR) {
							if (selectedCharacter != null) {
								int targetRow = (int) Math.round(targetQuad.getZ() - room.originz);
								int targetCol = (int) Math.round(targetQuad.getX() - room.originx);
								LinkedList<int[]> path = room.findPath(selectedCharacter.gridRow,
										selectedCharacter.gridCol, targetRow, targetCol, true);
								if (path != null) {
									manager.commandMove(selectedCharacter, path);
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

}
