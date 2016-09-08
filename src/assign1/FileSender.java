package assign1;

//Name: Luah Bao Jun (A0126258A)
//Assignment 1

import java.net.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

class FileSender {

	public static final int MAX_SIZE = 1000;

	public DatagramSocket socket; 
	public DatagramPacket pkt;

	public static void main(String[] args) {

		// check if the number of command line argument is 4
		if (args.length != 4) {
			System.out.println("Usage: java FileSender <path/filename> "
					+ "<rcvHostName> <rcvPort> <rcvFileName>");
			System.exit(1);
		}

		new FileSender(args[0], args[1], args[2], args[3]);
	}

	public FileSender(String fileToOpen, String host, String port, String rcvFileName) {

		// Refer to Assignment 0 Ex #4 on how to open a file with BufferedInputStream

		// UDP transmission is unreliable. Sender may overrun
		// receiver if sending too fast, giving packet lost as a result.
		// It is suggested that sender pause for a while after sending every packet:
		// E.g., Thread.sleep(1); // pause for 1 millisecond
		// On the other hand, don't pause more than 10ms after sending
		// a packet, or your program will take a long time to send a small file.
		
		try {
			socket = new DatagramSocket();
			File source = new File(fileToOpen);
			InetAddress ipAddress = InetAddress.getByName(host);
			int portNo = Integer.parseInt(port);
			
			// Send over the file name packet to the receiver
			byte[] fileName = rcvFileName.getBytes();
			pkt = new DatagramPacket(fileName, fileName.length, ipAddress, portNo); 
			socket.send(pkt);
			Thread.sleep(1);
			
			// Send over the file size packet to the receiver
			byte[] fileSize = (String.valueOf(source.length())).getBytes();
			pkt = new DatagramPacket(fileSize, fileSize.length, ipAddress, portNo);
			socket.send(pkt);
			Thread.sleep(1);

			// Send over individual file packets in at most 1000 bytes each time
			// Only terminate when the entire file has sent over to the receiver
			FileInputStream fis = new FileInputStream(source);
			BufferedInputStream bis = new BufferedInputStream(fis);
			byte[] buffer = new byte[MAX_SIZE];
			int numBytes = 0;
			
			while((numBytes = bis.read(buffer)) > 0) {
				pkt = new DatagramPacket(buffer, numBytes, ipAddress, portNo);
				socket.send(pkt);
				Thread.sleep(1);
				numBytes += pkt.getLength();
			}

			bis.close();
			socket.close();

			System.out.println("File is sent successfully.");
		
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}	
	}
}