package pambatch.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import pambatch.BatchControl;
import pambatch.config.BatchCommand;
import pambatch.config.BatchParameters;

public class BatchMulticastController {

	private BatchControl batchControl;

	private DatagramSocket datagramSocket;

	private InetAddress mcIPAddress;

	private Thread rxThread;

	private volatile boolean continueRX;

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
			try {
				socket.receive(packet);
				interpretPacket(packet);
			} catch (IOException e) {
			}
		}
	}

	private void interpretPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		if (data == null) {
			System.out.println("Multicastcontroller recieved null data");
		}
		String str = new String(data);
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

}
