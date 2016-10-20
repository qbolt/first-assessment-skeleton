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
	private Map<String, String> commandsMap = new HashMap<String, String>();

	public Commands() {
		try {
			Object obj = parser.parse(new FileReader("commands.json"));
			JSONObject jsonObject = (JSONObject) obj;

			JSONObject commands = (JSONObject) jsonObject.get("commands");

			for (Object command : commands.keySet()) {
				commandsMap.put((String) command, (String) commands.get(command));
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public String getCommand(String command) {
		return commandsMap.get(command);
	}

	public String getAllCommands() {
		StringBuilder allCommands = new StringBuilder();
		commandsMap
				.forEach((command, description) -> allCommands.append("    " + command + ": " + description + "\n\n"));
		return allCommands.toString();
	}

}
