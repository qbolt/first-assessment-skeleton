package com.cooksys.assessment.server;

import com.cooksys.assessment.model.Message;

public class ClientInfo {
	private String username;
	private String lastCommand;
	
	public void setInfo(Message message) {
		this.username = message.getUsername();
		this.lastCommand = message.getCommand();
	}
	
	public String getLastCommand() {
		return this.lastCommand;
	}
	
	public String getUsername() {
		return this.username;
	}

}
