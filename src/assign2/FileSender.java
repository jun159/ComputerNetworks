package assign2;

//Name: Luah Bao Jun (A0126258A)
//Assignment 2

import java.net.*;
import java.util.zip.CRC32;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

public class FileSender {

	private static final boolean DEBUG = false;
	private static final int TIMEOUT = 5;
	private static final int MAX_SIZE = 1000;
	private static final int MAX_DATA_SIZE = 970;
	private static final int OFFSET = 8;
	private static final int OFFSET_DATA = 4;
	private static final int MULTIPLE = 10;

	private static boolean terminateProgram = false;

	public DatagramSocket socket; 
	public DatagramPacket pkt;
	public DatagramPacket ackPkt;

	private InetAddress _ipAddress;
	private int _portNo;
	private long startTime;

	private ByteArrayOutputStream bos;

	public static void main(String[] args) {

		// check if the number of command line argument is 4
		if (args.length != 4) {
			System.out.println("Usage: java FileSender <path/filename> "
					+ "<rcvHostName> <rcvPort> <rcvFileName>");
			System.exit(1);
		}

		new FileSender(args[0], args[1], args[2], args[3]);
	}

	private void sendFileName(String fileName) throws Exception {

		while (true) {
			socket.setSoTimeout(TIMEOUT);
			
			try {
				socket.receive(ackPkt);
				String rcvrFileName = new String(ackPkt.getData(), 0, ackPkt.getLength());
				// Make sure the file name received by receiver is not corrupted
				// Else resend the file name packet
				if (fileName.equals(rcvrFileName)) {
					// Inform receiver file name is not corrupted
					if(DEBUG) System.out.println("File name is sent successfully" + fileName);
					byte[] buffer = new byte[1];
 				pkt = new DatagramPacket(buffer, buffer.length, _ipAddress, _portNo);
 				for(int i = 0; i < MULTIPLE; i++) {
 					socket.send(pkt);
 				}
					break;
				} else {
					if(DEBUG) System.out.println("Resend packet due to corruption");
					socket.send(pkt);
				}
			} catch (Exception e) {
				socket.send(pkt);
			}
		}
	}

	private void addSeqNumber(int seqNumber) throws Exception {

		bos.write(ByteBuffer.allocate(OFFSET).putInt(seqNumber).array());
	}

	private byte[] addPayload(BufferedInputStream bis, byte[] packetByte, int fileSize, 
		int expectedACK, int seqNumber, int currSize, int currByte) throws Exception {

		if (expectedACK == seqNumber) {
			if(terminateProgram) {
				terminate();
			} else if (fileSize < currByte + currSize) {
				// All packets successfully sent, terminate the next round
				terminateProgram = true;
				int lastPktSize = fileSize - currByte;
				packetByte = new byte[lastPktSize];
				bis.read(packetByte);
				currSize = fileSize - currByte;
			} else {
				bis.read(packetByte);
			}
		}

		bos.write(ByteBuffer.allocate(OFFSET_DATA).putInt(currSize).array());
		bos.write(packetByte);

		return packetByte;
	}

	private void addChecksumHeader(byte[] packetByte) throws Exception {

		CRC32 crc = new CRC32();
		crc.update(packetByte);
		long checksum = crc.getValue();
		bos.write(ByteBuffer.allocate(OFFSET).putLong(checksum).array());
	}

	private void terminate() throws Exception {

		byte[] lastByte = new byte[0];
 	pkt = new DatagramPacket(lastByte, lastByte.length, _ipAddress, _portNo);
 	
 	for(int i = 0; i < MULTIPLE; i++) {
 		socket.send(pkt);
 	}

		socket.close();
		bos.close();

		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;
		System.out.println("File is sent successfully");
		System.out.println("Total execution time: " + totalTime + " seconds");
		System.exit(0);
	}

	public FileSender(String fileToOpen, String host, String port, String rcvFileName) {

		startTime = System.currentTimeMillis();
		byte[] buffer = new byte[MAX_SIZE];
		byte[] dataBuffer = new byte[MAX_DATA_SIZE];
		byte[] sendBuffer = new byte[MAX_DATA_SIZE];
		int fileSize = (int) (new File(fileToOpen).length());
		int currSize = MAX_DATA_SIZE;
		int seqNumber = 0;
		int expectedACK = 0;
		int currByte = 0;
		boolean last = false;

		try {
			File source = new File(fileToOpen);
			socket = new DatagramSocket();
			ackPkt = new DatagramPacket(buffer, buffer.length);
			InetAddress ipAddress = InetAddress.getByName(host);
			int portNo = Integer.parseInt(port);
			_ipAddress = ipAddress;
			_portNo = portNo;
			FileInputStream fis = new FileInputStream(fileToOpen);
			BufferedInputStream bis = new BufferedInputStream(fis);

			// (1) Send over the file name packet to the receiver
			byte[] fileName = rcvFileName.getBytes();
			pkt = new DatagramPacket(fileName, fileName.length, ipAddress, portNo);
			socket.send(pkt);
			// Keep sending until the packet is successfully received
			sendFileName(rcvFileName);

			// (2) Add new header items to every packet
			while (true) {			
				// New packet, add all new items to header
				bos = new ByteArrayOutputStream();
				addSeqNumber(seqNumber);
				currByte = seqNumber * currSize;
				dataBuffer = addPayload(bis, dataBuffer, fileSize, 
					expectedACK, seqNumber, currSize, currByte);
				addChecksumHeader(dataBuffer);
				sendBuffer = bos.toByteArray();

				// Send over individual file packets in at most 1000 bytes each time
				// Only terminate when the entire file has sent over to the receiver
				pkt = new DatagramPacket(sendBuffer, sendBuffer.length, ipAddress, portNo);
				seqNumber++;

				while(true) {
					socket.setSoTimeout(TIMEOUT);
					socket.send(pkt);

					try {
						// Received acknowledgment packet from receiver
						socket.receive(ackPkt);

						int ackNumber = ByteBuffer.wrap(buffer).getInt();
						if (ackNumber == expectedACK) {
							if(DEBUG) System.out.println("Expected ACK received: " + expectedACK);
							expectedACK = seqNumber;
							for(int i = 0; i < MULTIPLE; i++) {
								socket.send(pkt);
							}
							break;
						} else {
							if(DEBUG) System.out.println("Received garbage ACK value: " + ackNumber);
							if(DEBUG) System.out.println("Resend packet: " + seqNumber);
						}
					} catch (InterruptedIOException e) {
						if(DEBUG) System.out.println(e.getMessage());
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}