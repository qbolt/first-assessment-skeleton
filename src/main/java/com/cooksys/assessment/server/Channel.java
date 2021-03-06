package com.cooksys.assessment.server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Channel {

	private static AtomicInteger numberOfChannels = new AtomicInteger(0);
	private ServerSocket serverSocket;
	private Executor executor;

	private String channelName;
	private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
	private BlockingQueue<Message> broadcastQueue = new LinkedBlockingQueue<>();

	public Channel(String channelName, Executor executor, ServerSocket serverSocket) {
		numberOfChannels.getAndIncrement();
		this.channelName = channelName;
		this.executor = executor;
		this.serverSocket = serverSocket;
		startBroadcaster();
	
	}

	public void removeClient(ClientHandler client) {
		if (clients.size() == 1) {
			clients.remove(client);
			client.closeChannel();
		}
		clients.remove(client);
	}

	public boolean addClient(ClientHandler client) {
		for (ClientHandler c : clients) {
			if (c.getUsername().equals(client.getUsername())) {
				return false;
			}
		}
		clients.add(client);
		return true;
	}

	public String getConnectedClientsAsString() {
		StringBuilder stringClients = new StringBuilder();

		for (ClientHandler client : clients) {
			stringClients.append(client.getUsername() + ((clients.indexOf(client) != clients.size() - 1) ? "\n" : ""));
		}

		return stringClients.toString();
	}

	public boolean sendPrivateMessage(Message message) {
		String recipient = message.getCommand().substring(1);

		for (ClientHandler client : clients) {
			if (client.getUsername().equals(recipient)) {
				client.queueMessage(message);
				client.setLastWhisper(message.getUsername());
				return true;
			}
		}
		return false;
	}

	public void broadcastMessage(Message message) throws JsonProcessingException {
		synchronized (clients) {
			broadcastQueue.add(message);
		}
	}

	public void startBroadcaster() {
		this.executor.execute(new Runnable() {

			@Override
			public void run() {
				while (!serverSocket.isClosed()) {
					try {
						Message message = broadcastQueue.take();
						synchronized (clients) {
							for (ClientHandler client : clients) {
								if (client.getUsername() != message.getUsername()) {
									client.queueMessage(message);
								}
							}
						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public boolean usernameIsTaken(String username) {
		for (ClientHandler client : clients) {
			if (client.getUsername().equals(username))
				return true;
		}
		return false;
	}

	public int getNumberOfClients() {
		return clients.size();
	}

	public String getChannelName() {
		return this.channelName;
	}
}
