package com.cooksys.assessment.server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Commands {

	private JSONParser parser = new JSONParser();
	private Map<String, String> channelCommandsMap = new HashMap<String, String>();
	private Map<String, String> lobbyCommandsMap = new HashMap<String, String>();

	public Commands() {
		try {
			Object obj = parser.parse(new FileReader("commands.json"));
			JSONObject jsonObject = (JSONObject) obj;
			JSONObject channelCommands = (JSONObject) jsonObject.get("channelCommands");
			JSONObject lobbyCommands = (JSONObject) jsonObject.get("lobbyCommands");

			for (Object command : channelCommands.keySet()) {
				channelCommandsMap.put((String) command, (String) channelCommands.get(command));
			}

			for (Object command : lobbyCommands.keySet()) {
				lobbyCommandsMap.put((String) command, (String) lobbyCommands.get(command));
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public String getAllChannelCommands() {
		StringBuilder allCommands = new StringBuilder();
		channelCommandsMap
				.forEach((command, description) -> allCommands.append("    " + command + ": " + description + "\n\n"));
		return allCommands.toString();
	}

	public String getAllLobbyCommands() {
		StringBuilder allCommands = new StringBuilder();
		lobbyCommandsMap
				.forEach((command, description) -> allCommands.append("    " + command + ": " + description + "\n\n"));
		return allCommands.toString();

	}
}
