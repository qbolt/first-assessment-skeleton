package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Runnable {

	private Logger log = LoggerFactory.getLogger(Server.class);
	private ExecutorService executor;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
	private int port;
	
	public Server(int port, ExecutorService executor) {
		super();
		this.executor = executor;
		this.port = port;
	}

	public void run() {
		log.info("server started");
		ServerSocket serverSocket;
		Channel mainChannel = new Channel("Default");
		try {
			serverSocket = new ServerSocket(this.port);
			while (true) {
				Socket socket = serverSocket.accept();
				ClientHandler client = new ClientHandler(socket, mainChannel, executor);
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
