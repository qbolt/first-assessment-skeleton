package com.cooksys.assessment.model;

import com.cooksys.assessment.server.Server;

public class Message {

	private String username = "";
	private String command = "";
	private String contents = "";

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

	public void formatConnectionMessageContents(String contents) {
		this.contents = ("" + Server.getCurrentTimeStamp() + ": <" + username + "> " + contents + this.contents);
	}

	public void formatContents(String contents) {
		this.contents = ("" + Server.getCurrentTimeStamp() + " <" + username + "> " + contents + this.contents);
	}

}
