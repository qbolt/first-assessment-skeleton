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
	private Commands commands = new Commands();
	private ServerSocket serverSocket;
	
	public Server(int port, ExecutorService executor) {
		super();
		this.executor = executor;
		this.port = port;
	}

	public void run() {
		log.info("server started");
		try {
			serverSocket = new ServerSocket(this.port);
			Channel channel = new Channel("Default", executor, serverSocket);
			
			while (true) {
				Socket socket = serverSocket.accept();
				ClientHandler client = new ClientHandler(socket, channel, executor, commands);
				executor.execute(client);
			}
			
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	public boolean isClosed() {
		return serverSocket.isClosed();
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
