package com.cooksys.assessment.model;

import com.cooksys.assessment.server.Server;

public class Message {

	private String username = "";
	private String command = "";
	private String contents = "";
	private String timestamp = "";

	public void setMessage(String username, String command, String contents) {
		this.username = username;
		this.command = command;
		this.contents = contents;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}
	
	public void setTimestamp() {
		this.timestamp = Server.getCurrentTimeStamp() + ": ";
	}
	
	public void setTimestamp(Long timestamp) {
		this.timestamp = Server.convertTimestampToString(timestamp) + " ";
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
}
