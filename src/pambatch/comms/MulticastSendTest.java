package pambatch.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class MulticastSendTest {


	int mcPort = BatchMulticastRX.mcPort;
	String mcIPStr = BatchMulticastRX.mcIPStr;
	private DatagramSocket rxSocket;	

	public static void main(String[] args) {
		new MulticastSendTest().run();
	}

	private void run() {
		try {
			DatagramSocket udpSocket = new DatagramSocket();
//			udpSocket.setSoTimeout(500);
			Thread rxt = new Thread(new Runnable() {

				@Override
				public void run() {
					receiveThread(udpSocket);
				}
			});
			rxt.start();

			/*
			 * Set up an asynchronous receiver. 
			 */

			InetAddress mcIPAddress = InetAddress.getByName(mcIPStr);
			DatagramPacket rxPacket = new DatagramPacket(new byte[1024] , 1024);
			for (int i = 0; i < 100; i ++) {
				String msg = String.format("Sending multicast %d", i);
				DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length());
				packet.setAddress(mcIPAddress);
				packet.setPort(mcPort);
				udpSocket.send(packet);
				int localPort = udpSocket.getLocalPort();
				if (i == 0) {
				}
				System.out.printf("Sent message %s on local port %d\n", msg, localPort);
				for (int j = 0; j < 0; j++) {
					long t1 = System.currentTimeMillis();
					try {
						udpSocket.receive(rxPacket);
					}
					catch (SocketTimeoutException timeout) {
						System.out.println("Timeout waiting for reply to message " + msg);
						continue;
					}

					long t2 = System.currentTimeMillis();
					System.out.printf("Reply to message %s after %d millis\n", msg, t2-t1);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			udpSocket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (rxSocket != null) {
			rxSocket.close();
		}
		System.out.println("Sent a  multicast message.");
		System.out.println("Exiting application");
	}

	protected void receiveThread(DatagramSocket rxSocket) {
		/*
		 * Set up a receive thread on a normal datagram socket. 
		 */

		InetAddress mcIPAddress = null;
		SocketAddress sockAddress;
		try {
//			mcIPAddress = InetAddress.getByName(mcIPStr);
//			sockAddress = new InetSocketAddress(mcIPAddress, localPort);
//			rxSocket = new DatagramSocket(localPort);
			System.out.println("Multicast Receiver running at:"
					+ rxSocket.getLocalSocketAddress());
//			rxSocket.joinGroup(sockAddress, null);
			DatagramPacket rxPacket = new DatagramPacket(new byte[1024], 1024);
			while (true) {
				rxSocket.receive(rxPacket);
				System.out.printf("Test recieved %s\n", new String(rxPacket.getData()));
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
