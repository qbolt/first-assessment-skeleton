package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {

	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private ObjectMapper mapper = new ObjectMapper();
	private ExecutorService executor;
	private ClientInfo clientInfo = new ClientInfo();
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private Channel currentChannel;

	private Socket socket;
	PrintWriter writer;
	BufferedReader reader;

	public ClientHandler(Socket socket, Channel channel, ExecutorService executor) {
		super();
		this.socket = socket;
		this.currentChannel = channel;
		this.executor = executor;
	}

	public void run() {
		try {

			mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			// Create thread to read messages from queue and send to client.
			executor.execute(new Runnable() {

				@Override
				public void run() {
					while (!socket.isClosed()) {
						try {
							String response = mapper.writeValueAsString(messageQueue.take());
							writer.write(response);
							writer.flush();
						} catch (JsonProcessingException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});

			// Read commands/messages and process them.
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				// Set clientInfo/previous command.
				clientInfo.setUsername(message.getUsername());
				processMessage(message);
				if (!message.getCommand().equals("users"))
					clientInfo.setLastCommand(message.getCommand());
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processMessage(Message message) throws InterruptedException, IOException {
		switch (message.getCommand()) {
		case "connect":
			log.info("user <{}> connected", message.getUsername());

			// Add client to channel list.
			currentChannel.addClient(this);

			// Notify channel that user has connected.
			message.formatContents("has connected. ");
			currentChannel.broadcastMessage(message);
			break;

		case "disconnect":
			log.info("user <{}> disconnected", this.getUsername());
			this.socket.close();

			// Remove client from channel list.
			log.info("user <{}> was removed from <{}>", this.getUsername(), currentChannel.getChannelName());
			currentChannel.removeClient(this);

			// Notify channel that user has disconnected.
			message.formatContents("has disconnected. ");
			currentChannel.broadcastMessage(message);
			break;

		// Broadcast message to all users in currentChannel
		case "broadcast":
			log.info("user <{}> broadcasted message <{}>", this.getUsername(), message.getContents());
			message.formatContents("(all): ");
			this.currentChannel.broadcastMessage(message);
			break;

		// Send list of users to client.
		case "users":
			log.info("user <{}> requested list of users.", this.getUsername());
			message.formatContents("\n" + currentChannel.getConnectedClientsAsString());
			this.queueMessage(message);
			break;

		// Echo message back to user.
		case "echo":
			log.info("user <{}> echoed message <{}>", this.getUsername(), message.getContents());
			message.formatContents("(echo): ");
			this.queueMessage(message);
			break;

		default:
			// Send a private message to specified user.
			// If user isn't in channel, send alert/error back to client.
			if (message.getCommand().substring(0, 1).equals("@")) {
				message.formatContents("(whisper): ");
				if (!currentChannel.sendPrivateMessage(message)) {
					log.info("could not find user <{}>", message.getCommand().substring(1));
					message.formatContents("(alert): ");
					message.setCommand("alert");
					queueMessage(message);
				}
				
			// If client didn't provide command, process message using the last command.
			} else if (!clientInfo.getLastCommand().equals("")) {
				String tmp = message.getCommand() + " " + message.getContents();
				message.setCommand(clientInfo.getLastCommand());
				message.setContents(tmp);
				processMessage(message);
				
			// Else, command not recognized.
			} else {
				message.setContents("Command not recognized. ");
				message.setCommand("");
				message.formatContents("(alert): ");
			}
		}
	}

	public synchronized void queueMessage(Message message) throws InterruptedException {
		log.info("Added message to queue for <{}> from <{}>", this.getUsername(), message.getUsername());
		messageQueue.put(message);
	}

	public String getUsername() {
		return clientInfo.getUsername();
	}
}
