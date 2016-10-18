package com.cooksys.assessment.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Channel {
	
	private static AtomicInteger numberOfChannels= new AtomicInteger(0);
	
	private String channelName;
	private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
	//private Queue<Message> broadcastQueue = new LinkedBlockingQueue<>();

	public Channel() {
		numberOfChannels.getAndIncrement();
		this.channelName = "NoName";
	}
	
	public Channel(String channelName) {
		numberOfChannels.getAndIncrement();
		this.channelName = channelName;
	}
	
	public void add(ClientHandler client) {
		clients.add(client);
	}

	public void broadcastMessage(Message message) throws JsonProcessingException {
		synchronized (clients) {
			for (ClientHandler client : clients) {
				client.writeMessage(message);
			}
		}
	}
}
