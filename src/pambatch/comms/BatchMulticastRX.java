package pambatch.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import pambatch.BatchControl;

/**
 * Makes a batch processing multicast receiver thread to get 
 * progress messages from batch processes into the batch controller. 
 * @author dg50
 *
 */
public class BatchMulticastRX {

	// largely from http://www.java2s.com/Tutorials/Java/Java_Network/0050__Java_Network_UDP_Multicast.htm

	private BatchControl batchControl;

	private volatile boolean keepGoing = true;

	public static final int mcPort = 12345;
	public static final String mcIPStr = "230.1.1.1";
	private MulticastSocket mcSocket = null;

	public BatchMulticastRX(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				runMulticastReceiver();
			}
		});
		t.start();
	}

	protected void runMulticastReceiver() {
		InetAddress mcIPAddress = null;
		SocketAddress sockAddress;
		try {
			mcIPAddress = InetAddress.getByName(mcIPStr);
			sockAddress = new InetSocketAddress(mcIPAddress, mcPort);
			mcSocket = new MulticastSocket(mcPort);
			System.out.println("Multicast Receiver running at:"
					+ mcSocket.getLocalSocketAddress());
			mcSocket.joinGroup(sockAddress, null);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
		while (keepGoing) {
			try {
				mcSocket.receive(packet);
			} catch (IOException e) {
				System.out.println("Batch Multicast receiver exception: " + e.getMessage());
				continue;
			}
			String msg = new String(packet.getData(), packet.getOffset(),
					packet.getLength());
			InetAddress senderAddr = packet.getAddress();
			int senderPort = packet.getPort();
			System.out.printf("[Batch Multicast  Receiver] Receive from %s port %d:: %s\n", senderAddr.getHostAddress(), senderPort, msg);
			if (Math.random() > -0.5) {
				String reply = String.format("Thanks for message %s", msg);
				DatagramPacket toSend = new DatagramPacket(reply.getBytes(), reply.length(), senderAddr, senderPort);
				try {
					DatagramSocket replySocket = new DatagramSocket();
					replySocket.send(toSend);
					replySocket.close();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public void stopReceiving() {
		keepGoing = false;
		mcSocket.close();
	}

}
