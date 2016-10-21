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
	private Commands commands;

	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;

	private String username = "";
	private String formattedUsername = "";
	private String lastCommand = "";
	private String lastWhisper = "";
	boolean connected = false;

	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private ChannelManager channelManager;
	private Channel currentChannel;
	private boolean inChannel = false;

	public ClientHandler(Socket socket, ExecutorService executor, Commands commands, ChannelManager channelManager) {
		super();
		this.socket = socket;
		this.executor = executor;
		this.commands = commands;
		this.channelManager = channelManager;
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
				setUsername(message.getUsername());

				// Process command based on if user is in channel or in lobby.
				if (!inChannel) {
					processLobbyCommand(message);
				} else {
					processChannelCommand(message);
				}

				// Set command to be lastCommand if in channel and if it makes
				// sense.
				if (message.getCommand().equals("broadcast") || message.getCommand().equals("echo")
						|| message.getCommand().length() > 1 && message.getCommand().substring(0, 1).equals("@")) {
					lastCommand = message.getCommand();
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processLobbyCommand(Message message) throws InterruptedException, IOException {
		switch (message.getCommand()) {
		case "connect":

			if (connected) {
				message.setCommand("invalid");
				processLobbyCommand(message);
			} else {

				connected = true;
				log.info("user <{}> joined lobby.", getUsername());

				// Start thread to send messages from queue to client.
				startMessageSender();
				sendLobbyWelcomeScreen();
			}

			break;

		case "disconnect":
			log.info("user <{}> disconnected", this.getUsername());
			this.socket.close();
			break;

		case "create":
			currentChannel = channelManager.createChannel(message, this);
			if (currentChannel == null) {
				this.queueMessage(message);
				inChannel = false;
			} else {
				inChannel = true;
				log.info("<{}> created new channel <{}>", getUsername(), currentChannel.getChannelName());
				this.queueMessage(new Message(getUsername(), "success", "Created and joined channel."));
				currentChannel.addClient(this);
			}
			break;

		case "join":
			currentChannel = channelManager.joinChannel(message, this);
			if (currentChannel == null) {
				inChannel = false;
				this.queueMessage(message);
			} else {
				// Notify that user connected.
				inChannel = true;
				log.info("<{}> created new channel <{}>", getUsername(), currentChannel.getChannelName());
				this.queueMessage(new Message(getUsername(), "success", "Joined channel."));
				currentChannel.broadcastMessage(
						new Message(getUsername(), "connect", formattedUsername + " has joined the channel."));
			}
			break;

		case "help":
			log.info("user <{}> requested help <{}>", this.getUsername());
			this.queueMessage(new Message(getUsername(), message.getCommand(),
					"\n  Available commands: \n\n" + commands.getAllLobbyCommands()));
			break;

		case "list":
		case "channels":
			channelManager.setChannelsInfoMessage(message);
			this.queueMessage(message);
			break;

		case "name":
		case "username":
			setUsername(message.getContents());
			this.queueMessage(new Message(getUsername(), "success", "Changed name to " + getUsername()));
			break;

		default:
			this.queueMessage(new Message(getUsername(), "alert",
					"Command not recognized. " + "Type <help> to view available commands."));
			break;
		}
	}

	private void processChannelCommand(Message message) throws InterruptedException, IOException {
		switch (message.getCommand()) {

		// Send list of users to client.
		case "users":
			log.info("user <{}> requested list of users.", this.getUsername());
			this.queueMessage(new Message(getUsername(), message.getCommand(),
					"Currently connected users: \n" + currentChannel.getConnectedClientsAsString()));
			break;

		// Returns user to lobby.
		case "leave":
			log.info("user <{}> was removed from <{}>", this.getUsername(), currentChannel.getChannelName());

			// Notify channel that client left.
			// Move user to lobby.
			this.currentChannel.removeClient(this);
			this.currentChannel.broadcastMessage(
					new Message(getUsername(), "disconnect", formattedUsername + " has left the channel."));
			this.inChannel = false;

			// Print lobby info to user.
			sendLobbyWelcomeScreen();
			break;

		// Disconnects client from server and removes from channel.
		case "disconnect":
			log.info("user <{}> disconnected", this.getUsername());
			this.socket.close();

			// Remove client from channel list.
			log.info("user <{}> was removed from <{}>", this.getUsername(), currentChannel.getChannelName());
			this.currentChannel.removeClient(this);

			// Notify channel that client has disconnected.
			this.currentChannel.broadcastMessage(
					new Message(getUsername(), "disconnect", formattedUsername + " has disconnected."));
			break;

		// Broadcast message to all users in currentChannel
		case "broadcast":
			log.info("user <{}> broadcasted message <{}>", this.getUsername(), message.getContents());
			this.currentChannel.broadcastMessage(new Message(getUsername(), message.getCommand(),
					formattedUsername + " (all): " + message.getContents(), message.getTimestamp()));
			break;

		// Echo message back to client.
		case "echo":
			log.info("user <{}> echoed message <{}>", this.getUsername(), message.getContents());
			this.queueMessage(new Message(getUsername(), message.getCommand(),
					formattedUsername + " (echo): " + message.getContents(), message.getTimestamp()));
			break;

		case "help":
			log.info("user <{}> requested help <{}>", this.getUsername());
			this.queueMessage(new Message(getUsername(), message.getCommand(),
					"\n  Available commands: \n\n" + commands.getAllChannelCommands()));
			break;

		// Replies to user who last whispered client.
		case "/r":
			if (lastWhisper.equals("")) {
				this.queueMessage(new Message(getUsername(), "alert", "No one has whispered you yet."));
			} else {
				message.setCommand(lastWhisper);
				processChannelCommand(message);
			}
			break;

		default:

			// Send a private message to specified user.
			// If user isn't in channel, send alert/error back to client.
			if (message.getCommand().substring(0, 1).equals("@")) {
				message.setContents(formattedUsername + " (whisper): " + message.getContents());
				if (!currentChannel.sendPrivateMessage(message)) {
					log.info("could not find user <{}>", message.getCommand().substring(1));
					this.queueMessage(new Message(getUsername(), "alert", "User not found."));
				}
			}

			// If client didn't provide command, process message using the
			// last command.
			else if (!lastCommand.equals("")) {

				log.info("Setting message command to <{}>", lastCommand);
				processChannelCommand(new Message(getUsername(), lastCommand,
						message.getCommand() + " " + message.getContents(), message.getTimestamp()));
			}

			// Else, command not recognized.
			else {
				this.queueMessage(new Message(getUsername(), "alert",
						"Command not recognized. " + "Type <help> to view available commands."));
			}
		}
	}

	// Sends available channel info and help message.
	private void sendLobbyWelcomeScreen() throws InterruptedException {
		Message message = new Message();
		message.setTimestamp();
		channelManager.setChannelsInfoMessage(message);
		this.queueMessage(message);
		Thread.sleep(10);
		this.queueMessage(
				new Message(getUsername(), "success", "Entered lobby. Type <help> to view available commands."));
	}

	// Thread to read messages from queue and send to client.
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

	public void closeChannel() {
		log.info("Closing channel");
		channelManager.removeChannel(currentChannel);
		inChannel = false;
	}

	public synchronized void queueMessage(Message message) {
		log.info("Added message to queue for <{}> from <{}>", this.getUsername(), getUsername());
		try {
			messageQueue.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setUsername(String username) {
		this.username = username;
		this.formattedUsername = "<" + username + ">";
	}

	public String getUsername() {
		return this.username;
	}

	public void setLastWhisper(String username) {
		this.lastWhisper = "@" + username;
	}
}
