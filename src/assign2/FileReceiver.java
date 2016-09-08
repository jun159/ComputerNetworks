package assign2;

//Name: Luah Bao Jun (A0126258A)
//Assignment 2

import java.net.*;
import java.util.zip.CRC32;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

public class FileReceiver {

	private static final boolean DEBUG = false;
	private static final String CORRUPT = "co";
	private static final int MAX_SIZE = 1000;
	private static final int MAX_DATA_SIZE = 970;
	private static final int OFFSET_SEQ = 8;
	private static final int OFFSET_DATA = 4;
	private static final int OFFSET_PAYLOAD = 12;
	private static final int OFFSET_CHECKSUM = 8;

	private static int currSeq = -1;

	private DatagramSocket socket;
	private DatagramPacket pkt;
	private DatagramPacket ackPkt;

	private InetAddress senderAddress;
	private int senderPort;
	private int portNo;
	private int payload;

	private Set<Integer> seqSet;
	private BufferedOutputStream bos;

	public static void main(String[] args) {

		// check if the number of command line argument is 3
		if (args.length < 1) {
			System.out.println("Usage: java FileSender <rcvPort> ");
			System.exit(1);
		}

		FileReceiver receiver = new FileReceiver(args[0]);
		receiver.receivePacket();
	}

	public FileReceiver(String localPort) {

		this.seqSet = new HashSet<Integer> ();
		this.portNo = Integer.parseInt(localPort);
	}

	private void receiveFileName() throws Exception {

		byte[] buffer = new byte[MAX_SIZE];
		String actualFileName = new String();

		// Receive file name from sender
		while (true) {
			socket.receive(ackPkt);
			senderAddress = ackPkt.getAddress();
			senderPort = ackPkt.getPort();

			String fileName = new String(ackPkt.getData(), 0, ackPkt.getLength());
				
			// File name is correct, not corrupted ^.^
			if (ackPkt.getLength() == 1) {
				if(DEBUG) System.out.println("File name received successfully: " + actualFileName);
				FileOutputStream fos = new FileOutputStream(actualFileName);
				bos = new BufferedOutputStream(fos);
				break;
			} else {
				// Send file name back to sender for sender to check
				// If the file name received is not corrupted 
				actualFileName = fileName;
				buffer = fileName.getBytes();
				if(buffer.length > 1000) {
					if(DEBUG) System.out.println("File name received is larger than 1000 bytes");
					buffer = CORRUPT.getBytes();
				}
				pkt = new DatagramPacket(buffer, buffer.length, senderAddress, senderPort);
		    	socket.send(pkt);
			}
		}
	}

	private boolean isSeqNumberValid(byte[] buffer) throws Exception {

		boolean validSeqNumber = false;
		byte[] seqByte = new byte[OFFSET_SEQ];
		
		seqByte = Arrays.copyOfRange(buffer, 0, OFFSET_SEQ);
		int seqNumber = ByteBuffer.wrap(seqByte).getInt();
		
		if (seqNumber - currSeq == 1) {
			validSeqNumber = true;
		}

		return validSeqNumber;
	}

	private boolean isPayloadValid(byte[] buffer) throws Exception {

		boolean validPayload = false;
		byte[] payloadByte = new byte[OFFSET_DATA];

		payloadByte = Arrays.copyOfRange(buffer, OFFSET_SEQ, OFFSET_PAYLOAD);
		payload = ByteBuffer.wrap(payloadByte).getInt();
		
		if (payload <= 1000) {
			validPayload = true;
		}

		return validPayload;
	}

	private boolean isChecksumValid(byte[] buffer) throws Exception {

		boolean validChecksum = false;
		byte[] checksumByte = new byte[OFFSET_CHECKSUM];
		int offset = payload + OFFSET_PAYLOAD;
		byte[] dataByte = new byte[payload];

		dataByte = Arrays.copyOfRange(buffer, OFFSET_PAYLOAD, offset);	
		checksumByte = Arrays.copyOfRange(buffer, offset, offset + OFFSET_SEQ);
		
		CRC32 crc = new CRC32();
		crc.update(dataByte);
		long senderChecksum = ByteBuffer.wrap(checksumByte).getLong();
		long checksum = crc.getValue();

		if (senderChecksum == checksum) {
			validChecksum = true;
			// Only write to file if packet is not duplicate
			if(!seqSet.contains(currSeq + 1)){
				bos.write(dataByte, 0, dataByte.length);
			}			
		}

		return validChecksum;
	}

	private void sendACKToSender(int ackNumber) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(ByteBuffer.allocate(OFFSET_SEQ).putInt(ackNumber).array());
		pkt = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, senderAddress, senderPort);
		socket.send(pkt);
	}

	private void terminate(int length) throws Exception {

		if (length == 0) {
			System.out.println("File is received successfully");
			socket.close();
			bos.close();
			System.exit(0);
		}
	}

	private void receivePacket () {
		
		try {
			byte[] buffer = new byte[MAX_SIZE];
			socket = new DatagramSocket(portNo);
			ackPkt = new DatagramPacket(buffer, buffer.length);

			// (1) Receive file name from sender
			receiveFileName();
			
			// (2) Receive data from sender
			while (true) {
				// Receive packet from sender
				ackPkt = new DatagramPacket(buffer, buffer.length);
				socket.receive(ackPkt);
				
				terminate(ackPkt.getLength());

				// (3) Retrieve sequence number, data and check if checksum is valid
				// If checksum is correct, increment currSeq
				if (isSeqNumberValid(buffer) && isPayloadValid(buffer) && isChecksumValid(buffer)) {
					currSeq++;
					seqSet.add(currSeq);
					if(DEBUG) System.out.println("Data is not corrupted: " + currSeq);
				} else {
					if(DEBUG) System.out.println("Data is corrupted: " + (currSeq + 1));
				}

				// Send ACK packet to sender
				if(DEBUG) System.out.println("Send ACK: " + currSeq);
				sendACKToSender(currSeq);
			}

		} catch (Exception e) {
			System.out.println("Unable to receive packets");
		} 
	}
}