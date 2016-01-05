




package com.joe.proceduralgame;

import com.joe.proceduralgame.Quad.Type;

import android.view.MotionEvent;

public class Controller {
	private GameGLView view;
	private DungeonManager manager;
	private DungeonRenderer renderer;
	
	public Character selectedCharacter = null;

	public Controller(GameGLView view, DungeonRenderer renderer, DungeonManager manager) {
		this.view = view;
		this.renderer = renderer;
		this.manager = manager;
	}

	public boolean onTouchEvent(MotionEvent e) {
		Room room = manager.currentRoom;
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			if (manager.neutral) {
				float nearX = (e.getX() / view.getWidth() - .5f) * renderer.nearWidth;
				float nearY = (-e.getY() / view.getHeight() + .5f) * renderer.nearHeight;
				Quad targetQuad = RaycastUtils.pick(room, renderer.mViewMatrix, nearX, nearY);
				if (targetQuad != null) {
					if (targetQuad.type == Type.CHARACTER) {
						Character character = RaycastUtils.quadToCharacter(manager.currentRoom, targetQuad);
						if (character.isPlayerSelectable()) {
							selectedCharacter = character;
						}
						selectedCharacter = character;
					} else if (targetQuad.type == Type.FLOOR) {
						if (selectedCharacter != null) {
							selectedCharacter.walkTo(targetQuad.getX(), targetQuad.getZ());
							selectedCharacter = null;
						}
					}
				}
			}
		}
		return true;
	}

}
