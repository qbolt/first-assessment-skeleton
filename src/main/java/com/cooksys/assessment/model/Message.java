package com.cooksys.assessment.model;

public class Message {

	private String username;
	private String command;
	private String contents;

	public Message(){}
	
	public Message(Message message, String lastCommand) {
		this.username = message.username;
		this.command = lastCommand;
		this.contents = message.contents;
	}
	
	public Message(String message, String username) {
		this.contents = message;
		this.username = username;
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

}
