package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;

public class Server implements Runnable {

	private Logger log = LoggerFactory.getLogger(Server.class);
	private ExecutorService executor;
	private int port;

	private Commands commands = new Commands();
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yy HH:mm:ss");

	public Server(int port, ExecutorService executor) {
		super();
		this.executor = executor;
		this.port = port;
	}

	public void run() {
		log.info("server started");
		try {
			ServerSocket serverSocket = new ServerSocket(this.port);
			ChannelManager channelManager = new ChannelManager(executor, serverSocket);
			while (true) {
				Socket socket = serverSocket.accept();
				ClientHandler client = new ClientHandler(socket, executor, commands, channelManager);
				executor.execute(client);
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

	public static String getCurrentTimeStamp() {
		Date now = new Date();
		return DATE_FORMAT.format(now);
	}

	public static String convertTimestampToString(long timestamp) {
		Date time = new Date(timestamp);
		return DATE_FORMAT.format(time);
	}

}
