package com.cooksys.assessment.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Channel {

	private static AtomicInteger numberOfChannels = new AtomicInteger(0);

	private String channelName;
	private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

	public Channel() {
		numberOfChannels.getAndIncrement();
		this.channelName = "NoName";
	}

	public Channel(String channelName) {
		numberOfChannels.getAndIncrement();
		this.channelName = channelName;
	}

	public String getConnectedClientsAsString() {
		StringBuilder stringClients = new StringBuilder();

		for (ClientHandler client : clients) {
			stringClients.append(client.getUsername() + "\n");
		}

		return stringClients.toString();
	}

	public void removeClient(ClientHandler client) {
		clients.remove(client);
	}

	public void addClient(ClientHandler client) {
		clients.add(client);
	}

	public boolean sendPrivateMessage(Message message) throws InterruptedException {
		String recipient = message.getCommand().substring(1);

		for (ClientHandler client : clients) {
			if (client.getUsername().equals(recipient)) {
				message.setCommand("whisper");
				client.queueMessage(message);
				return true;
			}
		}
		return false;
	}

	public void broadcastMessage(Message message) throws JsonProcessingException, InterruptedException {
		synchronized (clients) {
			for (ClientHandler client : clients) {
				if (client.getUsername() != message.getUsername()) {
					client.queueMessage(message);
				}
			}
		}
	}

	public String getChannelName() {
		return this.channelName;
	}
}
