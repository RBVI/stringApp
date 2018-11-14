package edu.ucsf.rbvi.stringApp.internal.utils;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.command.AvailableCommands;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class OpenCyBrowser {
		public static boolean openURL(StringManager manager, String id, String url) {
			if (!manager.haveCyBrowser())
				return false;

			if (id == null)
				id = "String";
			Map<String, Object> args = new HashMap<>();
			args.put("url", url);
			args.put("id", id);
			args.put("newTab", "true");
			manager.executeCommand("cybrowser", "dialog", args, null);
			return true;
		}

}
