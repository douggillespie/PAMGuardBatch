package pambatch.remote;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import pambatch.BatchControl;
import pambatch.config.BatchParameters;
import pambatch.ctrl.BatchState;
import pambatch.ctrl.BatchStateObserver;

/**
 * functions for finding and handling interactions with remote agents
 * these are instances of a modified PAMDog running on remote computers which
 * can handle processing batch jobs on their own machines. 
 * @author dg50
 *
 */
public class RemoteAgentHandler implements BatchStateObserver {

	private BatchControl batchControl;
	
	private Timer agentPingTimer;

	private MulticastSocket datagramSocket;

	private InetAddress mcIPAddress;
	
//	InetAddress group;

	private Thread rxThread;

	private volatile boolean continueRX;

	// fixed port and addr. Same hard wired into PAMDog. May make optional later.
	public static final int defaultAgentPort = 12347;	
	public static final String defaultAgentAddr = "230.1.1.1";
	
	private RemoteAgentDataBlock remoteAgentDataBlock;

	private volatile boolean waitingReply;

	private volatile String replyString;

	private NetworkInterface currentNetInterface;
		
	public RemoteAgentHandler(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		
		this.remoteAgentDataBlock = new RemoteAgentDataBlock(this, batchControl.getBatchProcess());
		
		/*
		 *  comment this for debugging so that local machine behaves the same as remotes (may want to do
		 *  that anyway ? to keep things consistent?).  
		 */
		addLocalMachine();
		
		batchControl.addStateObserver(this);
		startListener();
				
		agentPingTimer = new Timer(10000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkRemoteAgents();
			}
		});
		agentPingTimer.setInitialDelay(0);
		agentPingTimer.start();
		
	}

	@Override
	public void update(BatchState batchState, Object data) {
		switch (batchState) {
		case INITIALISATIONCOMPLETE:
		case NEWSETTING:
			restartSocket();
		}
	}

	/**
	 * Make a dataunit for the local machine and add it to the list. 
	 * do this once at startup so it's always first in the list. 
	 */
	private void addLocalMachine() {

		// get the basic data about the computer. This only needs to be done once. 
		String computerName = BatchControl.getLocalMachineName();
		String osName = System.getProperty("os.name");
		String osArch = System.getProperty("os.arch");
		int nProcessors = Runtime.getRuntime().availableProcessors();
		RemoteAgentDataUnit rad = new RemoteAgentDataUnit(System.currentTimeMillis(), true, "localhost", computerName, osName, osArch, nProcessors);
		remoteAgentDataBlock.addPamData(rad);
	}

	private void startListener() {
		rxThread = new Thread(new Runnable() {
			@Override
			public void run() {
				receiveReplies();
			}

		}, "Batch multicast receiver");
		rxThread.start();		
	}

	protected void checkRemoteAgents() {
		
		sendCommand("hello");
	}

	/**
	 * Send data and then wait for a reply on the same socket. 
	 * @param data
	 * @param timeoutMillis
	 * @return
	 */
	public synchronized String sendAndWait(byte[] data, long timeoutMillis) {
		return sendAndWait(data, data.length, timeoutMillis);
	}
	
	/**
	 * Send data and then wait for a reply on the same socket. 
	 * Bit tricky since the socket is already waiting asynchronously. 
	 * @param data
	 * @param length data length (if less than data.length)
	 * @param timeoutMillis
	 * @return
	 */
	public synchronized String sendAndWait(byte[] data, int length, long timeoutMillis) {
		waitingReply = true;
		replyString = null;
		boolean sent = sendData(data, length);
		if (sent == false) {
			waitingReply = false;
			return null;
		}
		long now = System.currentTimeMillis();
		while (waitingReply && System.currentTimeMillis()-now < timeoutMillis) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return replyString;		
	}

	
	public synchronized boolean sendCommand(String command) {
		byte[] data = command.getBytes();
		return sendData(data);
	}
	
	public synchronized boolean sendData(byte[] data) {
		return sendData(data, data.length);
	}

	public synchronized boolean sendData(byte[] data, int length) {
		
//		System.out.println("Sending agent command " + command);
		
		DatagramSocket udpSocket = getSocket();
		if (udpSocket == null) {
			return false;
		}
		
		DatagramPacket packet = new DatagramPacket(data, length, mcIPAddress, defaultAgentPort);;
//		packet.setAddress(mcIPAddress);
//		packet.setPort(defaultAgentPort);
		try {
			udpSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Batch Multicastcontroller Error sending data: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Thread listening for asynchronous replies from remote agents. 
	 */
	private void receiveReplies() {
		continueRX = true;
		int len = 1024;
		DatagramPacket packet = new DatagramPacket(new byte[len], len);
		while (continueRX) {
			try {
				DatagramSocket socket = getSocket();
				if (socket == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				socket.receive(packet);
				interpretPacket(packet);
			} catch (IOException e) {
			}
		}
	}

	private void interpretPacket(DatagramPacket packet) {
		if (waitingReply) {
			String str = new String(packet.getData(), 0, packet.getLength());
//			System.out.println(str);
			replyString = new String(packet.getData(), 0, packet.getLength());
			waitingReply = false;
		}
		else {
			String rxStr = new String(packet.getData(), 0, packet.getLength());
			//		System.out.println("RX from remote agent: " + rxStr);
			String[] bits = rxStr.split(",");
			if (bits[0].equalsIgnoreCase("helloback")) {
				useHelloBack(packet);
			}
		}
	}

	/**
	 * This is a message received from PAMDog which should be running on remote machines which can share jobs with this one. 
	 * Jobs will only be able to run on the remote machine if data are in a shared network resource of some type. Not sure how
	 * I'm going to be able to check that at the moment - probably ask the remote host if it can access the data folder before 
	 * starting a job. 
	 * @param packet
	 */
	private void useHelloBack(DatagramPacket packet) {
		String rxStr = new String(packet.getData(), 0, packet.getLength());
		String[] bits = rxStr.split(",");
		String computerName = bits[1];
		String osName = bits[2];
		String osArch = bits[3];
		int nProc = 0;
		try {
			nProc = Integer.valueOf(bits[4]);
		}
		catch (NumberFormatException e) {
			System.out.println("Remote Agent Handler helloback invalid number of processors " + e.getMessage());
			return;
		}
		SocketAddress remoteAddr = packet.getSocketAddress();
		String remoteIp = remoteAddr.toString();
		RemoteAgentDataUnit exDataUnit = remoteAgentDataBlock.findByComputerName(computerName);
		if (exDataUnit == null) {
			exDataUnit = new RemoteAgentDataUnit(System.currentTimeMillis(), false, remoteIp, computerName, osName, osArch, nProc);
			remoteAgentDataBlock.addPamData(exDataUnit);
			// can't so this here or it blocks the receive thread so can't get the reply when it sends psfx data
			SwingUtilities.invokeLater(new PSFXSendTest(exDataUnit));
		}
		else {
			remoteAgentDataBlock.updatePamData(exDataUnit, System.currentTimeMillis());
		}
	}
	
	private class PSFXSendTest implements Runnable {

		private RemoteAgentDataUnit exDataUnit;

		public PSFXSendTest(RemoteAgentDataUnit exDataUnit) {
			this.exDataUnit = exDataUnit;
		}

		@Override
		public void run() {
			sendPSFXFile(exDataUnit);
		}
		
	}
	
	/**
	 * Send the content of the psfx file to the remote agent as a series
	 * of UDP messages. These will have to use broadcast to avoid opening
	 * another socket, so will have to identify with the remote name. 
	 * @param remoteAgent
	 * @return
	 */
	public boolean sendPSFXFile(RemoteAgentDataUnit remoteAgent) {
		int maxDataSize = 60000; // max total packet length is 65507 but we need to add a header.  
		String psfxName = batchControl.getBatchParameters().getMasterPSFX();
		if (psfxName == null) {
			return false;
		}
		File psfxFile = new File(psfxName);
		if (psfxFile.exists() == false) {
			System.out.println("SendPSFXFile: PSFX File doesn't exist. " + psfxName);
		}
		/**
		 * Read in the whole file to an array so that we know how many datagram packets we're 
		 * going to have to send. 
		 */
		ArrayList<byte[]> psfxBits = new ArrayList<>();
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(psfxFile));
			while (true) {
				byte[] data = new byte[maxDataSize];
				int bytesRead = bis.read(data);
				if (bytesRead <= 0) {
					break;
				}
				if (bytesRead < data.length) {
					data = Arrays.copyOf(data, bytesRead);
				}
				psfxBits.add(data);
				if (bytesRead < maxDataSize) {
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		// now make up data arrays and send them for each, waiting for an acknowlegement
		String retMsg = null;
		for (int i = 0; i < psfxBits.size(); i++) {
			byte[] data = psfxBits.get(i);
			byte chkSum = 0;
			for (int j = 0; j < data.length; j++) {
				chkSum ^= data[j];
			}
			String head = String.format("psfxdata,%s,%d,%d,%02X,", remoteAgent.getComputerName(), i+1, psfxBits.size(),chkSum);
			int headLen = head.length();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(headLen + data.length);
			bos.writeBytes(head.getBytes());
			bos.writeBytes(data);
			byte[] dataOut = bos.toByteArray();
			String response = sendAndWait(dataOut, 3000);
			if (response == null) {
				System.out.printf("Sending psfx data part %d of %d to remote PC %s failed\n", i+1, psfxBits.size(), remoteAgent.getComputerName());
				return false;
			}
			else {
				retMsg = response;//new String(response.getData(), 0, response.getLength());
				if (retMsg.contains("Error")) {
					System.out.printf("Error '%s' sending psfx part %d of %d to %s\n", retMsg, i+1, psfxBits.size(), remoteAgent.getComputerName());
				}
			}
		}
		if (retMsg == null) {
			System.out.println("No psfx messages sent to host " + remoteAgent.getComputerName());
			return false;
		}
		if (retMsg.equals("PSFXOK")) {
			System.out.printf("psfx file sucessfully transferred to host %s in %d parts\n", remoteAgent.getComputerName(), psfxBits.size());
			return true;
		}
		else {
			System.out.printf("Unknown PSFX return code '%s' from host %s\n", retMsg, remoteAgent.getComputerName());
			return false;
		}
		
	}

	private synchronized void restartSocket() {
		if (datagramSocket == null) {
			return;
		}
		datagramSocket.close();
		datagramSocket = null;
	}

	/**
	 * Check we've a valid and open datagram socket. 
	 * @return
	 */
	private synchronized DatagramSocket getSocket() {
		if (needNewsocket() == false) {
			return datagramSocket;
		}
//		BatchParameters params = batchControl.getBatchParameters();
		// open a socket. 
		try {
			datagramSocket = new MulticastSocket();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// interpret the address to save doing this multiple times. 
		try {
			mcIPAddress = InetAddress.getByName(defaultAgentAddr);
//			group  = 
		} catch (UnknownHostException e) {
			System.err.println("Batch Multicast agent controller Error with address: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		InetSocketAddress group = new InetSocketAddress(mcIPAddress, defaultAgentPort);
		String netInfName = batchControl.getBatchParameters().getNetworkInterfaceName();
		boolean available = NetInterfaceFinder.isAvailable(netInfName);
		if (available == false) {
			System.out.printf("Network interface %s is not currently available. Check your hardware\n", netInfName);
		}
		else {
			try {
				currentNetInterface = NetworkInterface.getByName(netInfName); // was eth8 on laptop in office using usbc adapter. 
				System.out.println("Connecting multiport socket on " + currentNetInterface.getDisplayName()); 
				//			datagramSocket.setBroadcast(true);
				datagramSocket.joinGroup(group, currentNetInterface);
				//			datagramSocket.b
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		launchReceiverThread();
		return datagramSocket;
	}

	private void launchReceiverThread() {
		// TODO Auto-generated method stub
		
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
	 * @return the remoteAgentDataBlock
	 */
	public RemoteAgentDataBlock getRemoteAgentDataBlock() {
		return remoteAgentDataBlock;
	}

}
