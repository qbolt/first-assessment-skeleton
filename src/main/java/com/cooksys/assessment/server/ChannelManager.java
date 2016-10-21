package com.cooksys.assessment.server;

import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.cooksys.assessment.model.Message;

public class ChannelManager {
	private ServerSocket serverSocket;
	private ExecutorService executor;
	private List<Channel> channels = new LinkedList<>();

	public ChannelManager(ExecutorService executor, ServerSocket serverSocket) {
		this.executor = executor;
		this.serverSocket = serverSocket;
	}

	public void setChannelsInfoMessage(Message message) {
		StringBuilder sb = new StringBuilder();
		if (channels.size() == 0) {
			sb.append("No available channels.\n\"create <channelname>\" to create a channel.");
			message.setMessage(message.getUsername(), "alert", sb.toString());
			return;
		} else {
			sb.append("\nAvailable Channels\nName: # of users connected.\n--------------\n");
			for (Channel channel : channels) {
				sb.append(channel.getChannelName() + ": (" + channel.getNumberOfClients() + ") users. \n");
			}
		}
		message.setMessage(message.getUsername(), "users", sb.toString());
	}

	public Channel createChannel(Message message, ClientHandler client) {
		if (channelNameIsTaken(message.getContents())) {
			return null;
		}

		// Create channel, add channel to list of channels, and return channel.
		Channel channel = new Channel(message.getContents(), executor, serverSocket);
		channels.add(channel);
		message.setMessage(client.getUsername(), "success", "Created and joined new channel.");
		return channel;
	}

	public Channel joinChannel(Message message, ClientHandler client) {
		for (Channel channel : channels) {

			// If channel exists and username is available, return channel.
			if (channel.getChannelName().equals(message.getContents())) {
				if (channel.addClient(client)) {
					return channel;
				} else {
					message.setMessage(client.getUsername(), "alert", "Username is taken.");
					return null;
				}
			}
		}
		message.setMessage(message.getUsername(), "alert", "Channel not found.");
		return null;
	}

	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

	private boolean channelNameIsTaken(String channelName) {
		for (Channel channel : channels) {
			if (channel.getChannelName().equals(channelName)) {
				return true;
			}
		}
		return false;
	}
}
