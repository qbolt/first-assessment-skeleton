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
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private Channel currentChannel;

	private String username = "";
	private String lastCommand = "";

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

			// Read commands/messages and process them.
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				// Save username and process command
				username = message.getUsername();
				processMessage(message);

				// Set command to be lastCommand if it makes sense to do so.
				if (!message.getCommand().equals("users") && !message.getCommand().equals("connect"))
					lastCommand = message.getCommand();
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
			// If client tries to connect with same username, notify client and
			// close socket.
			if (currentChannel.usernameIsTaken(message.getUsername())) {
				log.info("<{}> name was taken", message.getUsername());
				message.setCommand("alert");
				message.setContents("Username is already taken. ");
				message.formatContents("(alert): ");
				String response = mapper.writeValueAsString(message);
				writer.write(response);
				writer.flush();
				this.socket.close();
				break;
			}

			// Create thread to read messages from queue and send to client.
			this.startMessageSender();

			// Add clientHandler to channel list.
			currentChannel.addClient(this);

			// Notify channel that client has connected.
			message.formatConnectionMessageContents("has connected. ");
			currentChannel.broadcastMessage(message);

			break;

		case "disconnect":
			log.info("user <{}> disconnected", this.getUsername());
			this.socket.close();

			// Remove client from channel list.
			log.info("user <{}> was removed from <{}>", this.getUsername(), currentChannel.getChannelName());
			currentChannel.removeClient(this);

			// Notify channel that user has disconnected.
			message.formatConnectionMessageContents("has disconnected. ");
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

			// If client didn't provide command, process message using the
			// last command.
			} else if (!lastCommand.equals("")) {
				String tmp = message.getCommand() + " " + message.getContents();
				message.setCommand(lastCommand);
				message.setContents(tmp);
				lastCommand = "";
				processMessage(message);

			// Else, command not recognized.
			} else {
				message.setContents("Command not recognized. ");
				message.setCommand("alert");
				message.formatContents("(alert): ");
				this.queueMessage(message);
			}
		}
	}

	private void startMessageSender() {
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
	}

	public synchronized void queueMessage(Message message) throws InterruptedException {
		log.info("Added message to queue for <{}> from <{}>", this.getUsername(), message.getUsername());
		messageQueue.put(message);
	}

	public String getUsername() {
		return this.username;
	}
}
