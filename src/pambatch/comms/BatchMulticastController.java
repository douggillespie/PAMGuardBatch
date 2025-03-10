package pambatch.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import dataGram.Datagram;
import pambatch.BatchControl;
import pambatch.config.BatchCommand;
import pambatch.config.BatchParameters;

public class BatchMulticastController {

	private BatchControl batchControl;

	private DatagramSocket datagramSocket;

	private InetAddress mcIPAddress;

	private Thread rxThread;

	private volatile boolean continueRX;

	private volatile boolean rxRunning = false;

	private boolean dummyReady = false;
	
	private String testString = "Multicast Networking test complete";

	public BatchMulticastController(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		getSocket(); // should initialise the receive thread. 
	}

	public boolean sendCommand(String command) {
		return sendCommand(command.getBytes());
	}

	public boolean sendCommand(byte[] data) {

		BatchParameters params = batchControl.getBatchParameters();
		DatagramSocket udpSocket = getSocket();
		if (udpSocket == null) {
			return false;
		}
		DatagramPacket packet = new DatagramPacket(data, data.length);
		packet.setAddress(mcIPAddress);
		packet.setPort(params.getMulticastPort());
		try {
			udpSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Batch Multicastcontroller Error sending data: " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Send a batch command targeted at a specific PAMGuard. These are all sent
	 * with a command "batchcommand" and will be recievied on multicast by all 
	 * listeners, but only the listener with the matching id will pass the 
	 * real command on to it's handler. 
	 * @param databaseIndex
	 * @param commandid
	 */
	public void targetCommand(int id1, int id2, String commandid) {
		String commandStr = formatBatchCommand(id1, id2, commandid);
		sendCommand(commandStr);
	}

	public String formatBatchCommand(int id1, int id2, String commandid) {
		String commandStr = String.format("%s %d %d %s", PamController.command.BatchCommand.commandId, id1,id2,commandid);
		return commandStr;
	}

	public byte[] createBinaryCommand(int id1, int id2, String command, byte[] data) {
		String commandStr = String.format("%s %d %d:::", PamController.command.BatchCommand.commandId, id1,id2);
		byte[] cmdDat = commandStr.getBytes();
		// and add the data to the end of that. 
		byte[] fullDat = Arrays.copyOf(cmdDat, cmdDat.length+data.length);
		for (int i = 0, j = cmdDat.length; i < data.length; i++, j++) {
			fullDat[j] = data[i];
		}
		return fullDat;
	}

	public void targetCommand(int id1, int id2, String command, byte[] data) {
		byte[] fullDat = createBinaryCommand(id1, id2, command, data);
		sendCommand(fullDat);
	}

	/**
	 * Check we've a valid and open datagram socket. 
	 * @return
	 */
	private synchronized DatagramSocket getSocket() {
		if (needNewsocket() == false) {
			return datagramSocket;
		}
		BatchParameters params = batchControl.getBatchParameters();
		// open a socket. 
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}
		// interpret the address to save doing this multiple times. 
		try {
			mcIPAddress = InetAddress.getByName(params.getMulticastAddress());
		} catch (UnknownHostException e) {
			System.err.println("Batch Multicastcontroller Error with address: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		launchReceiverThread();
		return datagramSocket;
	}

	private boolean needNewsocket() {
		if (datagramSocket == null) {
			return true;
		}
		if (datagramSocket.isClosed()) {
			return true;
		}
		// maybe check ports here too ...
		//		if (datagramSocket.)
		return false;
	}

	/**
	 * Launch a receiver thread using the same socket port so that 
	 * we can get messages back asynchronously. 
	 */
	private void launchReceiverThread() {
		rxThread = new Thread(new Runnable() {
			@Override
			public void run() {
				receiveReplies();
			}
		}, "Batch multicast receiver");
		rxThread.start();

		Thread dumThread = new Thread(new DummyRemote());
		dumThread.start();
		
		/* 
		 * now send a test message to try to trigger the firewall so that this
		 * occurs early and we can get permissions set up now. 
		 */
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				triggerFirewall();				
			}
		});
	}

	/**
	 * now send a test message to try to trigger the firewall so that this
	 * occurs early and we can get permissions set up now. 
	 */
	private void triggerFirewall() {
		// wait to see if the thread is running. Give it two seconds, which is way more than enough
		for (int i = 0; i < 10; i++) {
			if (dummyReady) {
				break;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
		}
		
		sendCommand(testString);
		// now send a single message to that port. 
//		DatagramPacket dataGram = new DatagramPacket(testString.getBytes(), testString.length());
//		dataGram.setAddress(mcIPAddress);
//		int port = sock.getLocalPort();
//		dataGram.setPort(batchControl.getBatchParameters().getMulticastPort());
////		dataGram.setSocketAddress(sock.getLocalSocketAddress());
//		try {
//			sock.send(dataGram);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * Loop to sit and wait for replies from remote PAMGuards. 
	 */
	protected void receiveReplies() {
		continueRX = true;
		DatagramSocket socket = getSocket();
		if (socket == null) {
			return;
		}
		int len = 1024;
		DatagramPacket packet = new DatagramPacket(new byte[len], len);
		while (continueRX) {
			rxRunning = true;
			try {
				socket.receive(packet);
				interpretPacket(packet);
			} catch (IOException e) {
			}
		}
		rxRunning = false;
	}

	private void interpretPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		if (data == null) {
			System.out.println("Multicastcontroller recieved null data");
		}
		String str = new String(data);
		str = str.trim();
		if (str.equals(testString)) {
			System.out.println(str);
			return;
		}
//		System.out.println("Multicast controller recieved " + str);
		batchControl.newStatusPacket(packet);

	}

	/**
	 * Shut everything down. 
	 */
	public void close() {
		continueRX = false;
		if (datagramSocket != null) {
			datagramSocket.close();
		}
		if (rxThread != null) {
			try {
				rxThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Dummy remote thread that's used once on startup to trigger the firewall warnings. 
	 * @author dg50
	 *
	 */
	private class DummyRemote implements Runnable {
		private MulticastSocket dummySocket;

		public void run() {
			BatchParameters params = batchControl.getBatchParameters();
			InetSocketAddress group = new InetSocketAddress(params.getMulticastAddress(), params.getMulticastPort());
			try {
				NetworkInterface netIf = NetworkInterface.getByName("bge0");
				dummySocket = new MulticastSocket(params.getMulticastPort());
				dummySocket.joinGroup(group, netIf);
				dummySocket.setSoTimeout(0);

				//			System.out.printf("Waiting for multicast messages at %s port %d\n", mAddress, mPort);
				int bLen = testString.length()+20;
				byte[] byteBuffer = new byte[bLen];

				//  sit in loop
				DatagramPacket datagram = new DatagramPacket(byteBuffer, bLen);
				dummyReady = true;
				dummySocket.receive(datagram);
				byte[] data = datagram.getData();
				String str = new String(data);
				System.out.println(str);
				dummyReady = false;
				// and send a reply since the other port may need enabled too. 
				DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length());
				packet.setAddress(datagram.getAddress());
				packet.setPort(datagram.getPort());
					dummySocket.send(packet);
			}
			catch (Exception ioE) {
				ioE.printStackTrace();
			}
		}
	}

}
