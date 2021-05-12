package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.ui.util.Icons;

import java.util.Arrays;
import java.util.List;

/**
 * Base window attributes.
 *
 * @author Matt Coley
 */
public abstract class WindowBase extends Stage {
	/**
	 * Create the scene and add the base stylesheets.
	 */
	protected void init() {
		setScene(createScene());
		getScene().setOnKeyPressed(this::onKeyPressed);
		addStylesheets(getScene().getStylesheets());
		getIcons().add(new Image(Icons.LOGO));
	}

	/**
	 * Handle window-level key events.
	 *
	 * @param event
	 * 		Key event.
	 */
	private void onKeyPressed(KeyEvent event) {
		KeybindConfig binds = Configs.keybinds();
		// TODO: Add functionality of window-level binds
	}

	/**
	 * @return Stage scene with prepared content.
	 */
	protected abstract Scene createScene();

	/**
	 * @param stylesheets
	 * 		Stylesheet list to update.
	 */
	public static void addStylesheets(List<String> stylesheets) {
		stylesheets.addAll(Arrays.asList("style/base.css",
				"style/code.css",
				"style/hierarchy.css",
				"style/log.css",
				"style/markdown.css",
				"style/scroll.css",
				"style/tabs.css",
				"style/text.css",
				"style/tree.css",
				"style/split.css")
		);
	}
}
