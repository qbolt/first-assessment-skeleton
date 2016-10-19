package com.cooksys.assessment.server;

import com.cooksys.assessment.model.Message;

public class ClientInfo {
	private String username = "";
	private String lastCommand = "";

	public void setInfo(Message message) {
		this.username = message.getUsername();
		this.lastCommand = message.getCommand();
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getLastCommand() {
		return this.lastCommand;
	}

	public String getUsername() {
		return this.username;
	}
	
	public void setLastCommand(String lastCommand) {
		this.lastCommand = lastCommand;
	}
	
	

}
