package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private ClientInfo clientInfo = new ClientInfo();
	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
	private Channel currentChannel;

	private Socket socket;
	PrintWriter writer;
	BufferedReader reader;

	ObjectMapper mapper = new ObjectMapper();

	public ClientHandler(Socket socket, Channel channel) {
		super();
		this.socket = socket;
		this.currentChannel = channel;
	}
	
	public void run() {
		try {

			mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			// Create thread to read messages from queue and send to client.
			new Thread(new Runnable() {

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
			}).start();
			
			// Read commands/messages and process them.
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				
				// Set clientInfo/previous command.
				clientInfo.setInfo(message);

				processMessage(message);
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
			break;
		case "disconnect":
			log.info("user <{}> disconnected", message.getUsername());
			this.socket.close();
			break;
		case "broadcast":
			log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
			currentChannel.broadcastMessage(message);
			break;
		case "echo":
			log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
			this.queueMessage(message);
			break;
		default:
			if (message.getCommand().substring(0, 1).equals("@")) {
				if (!currentChannel.sendPrivateMessage(message)) {
					queueMessage(new Message("Client not found.", "Server"));
				}
			} else {
				processMessage(new Message(message, clientInfo.getLastCommand()));
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
