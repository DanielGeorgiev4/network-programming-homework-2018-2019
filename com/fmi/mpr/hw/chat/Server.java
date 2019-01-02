package com.fmi.mpr.hw.chat;

//using HashSet
import java.util.HashSet;
import java.util.Set;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetAddress;

public class Server {

	final static String MULTICAST_IP_ADDRESS = "224.0.0.3";
	final static int MULTICAST_PORT = 3000;
	final static int SERVER_PORT = 8080;
	final static int BUFFER_SIZE = 4096*3;
	final static String REGISTER_PREFIX = ">";
	final static String LOGOUT_PREFIX = "<";
	private Set<String> connectedUsers;
	public Server() throws IOException {
		connectedUsers = new HashSet<String>();
		doWork();
	}
	private void doWork() throws IOException {
		try (DatagramSocket receiveSocket = new DatagramSocket(SERVER_PORT)) {
			System.out.println("A chat room server has just been started started!");
			while (true) 
			{
				byte[] byteData = new byte[BUFFER_SIZE];
				// This is the empty packet
				DatagramPacket receivedPacketFromClient = new DatagramPacket(byteData, byteData.length);
				// This is the filled packet
				receiveSocket.receive(receivedPacketFromClient);
				boolean sendToMulticast = true,
						isRegisterLogout = false;
				String messageReceivedFromClient = new String(receivedPacketFromClient.getData(), 
															  receivedPacketFromClient.getOffset(), 
															  receivedPacketFromClient.getLength()),
					    nameOfClient = null;

				if (messageReceivedFromClient.startsWith(REGISTER_PREFIX)) {
					nameOfClient = messageReceivedFromClient.substring(REGISTER_PREFIX.length());
					sendToMulticast = !connectedUsers.contains(nameOfClient);
					registerClient(nameOfClient, receiveSocket, receivedPacketFromClient.getAddress(), receivedPacketFromClient.getPort());		
					messageReceivedFromClient = nameOfClient + " has just entered the chat room...";
					isRegisterLogout = true;
				}
				else if (messageReceivedFromClient.startsWith(LOGOUT_PREFIX)) {
					nameOfClient = messageReceivedFromClient.substring(LOGOUT_PREFIX.length());
					logoutClient(nameOfClient);
					messageReceivedFromClient = nameOfClient+ " left the chat room...";
					isRegisterLogout = true;
				} 
				else {
					//spliting by ':'
					nameOfClient = messageReceivedFromClient.split(":")[0];
				}

				if (sendToMulticast && (isRegisterLogout || connectedUsers.contains(nameOfClient))) {
					byte[] bytesForMulticast;
					if(isRegisterLogout)
						bytesForMulticast = messageReceivedFromClient.getBytes();
					else 
						bytesForMulticast = receivedPacketFromClient.getData();
					InetAddress address = InetAddress.getByName(MULTICAST_IP_ADDRESS);
					DatagramPacket multicastPacket = new DatagramPacket(bytesForMulticast, bytesForMulticast.length, address,
							MULTICAST_PORT);
					receiveSocket.send(multicastPacket);
				}
			}
		}

	}
	//making registration
	private void registerClient(String nameOfClient, DatagramSocket socket, InetAddress address, int port) throws IOException {
		String registerResponse;
		if (connectedUsers.contains(nameOfClient)) {
			registerResponse = "This name has already been taken!";
		} else {
			registerResponse = "Success";
			connectedUsers.add(nameOfClient);
			System.out.println(nameOfClient + " has just entered the chat room!");
		}
		DatagramPacket registerResponsePacket = new DatagramPacket(registerResponse.getBytes(),
																   registerResponse.getBytes().length,
																   address, 
																   port);
		socket.send(registerResponsePacket);
	}
	// client logging in
	private void logoutClient(String nameOfClient) {
		if (connectedUsers.contains(nameOfClient)) {
			connectedUsers.remove(nameOfClient);
			System.out.println(nameOfClient + " has just left the chat room");
		}
	}

	public static void main(String[] args) throws IOException {
		//Just creating the server
		Server chatServer = new Server();
	}

}