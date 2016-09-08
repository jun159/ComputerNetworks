package assign1;

//Name: Luah Bao Jun (A0126258A)
//Assignment 1

import java.net.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

class FileReceiver {

	public static final int MAX_SIZE = 1000;

	public DatagramSocket socket; 
	public DatagramPacket pkt;

	public static void main(String[] args) {

		// check if the number of command line argument is 1
		if (args.length != 1) {
			System.out.println("Usage: java FileReceiver port");
			System.exit(1);
		}

		new FileReceiver(args[0]);
	}

	public FileReceiver(String localPort) {
	
		try {
			int portNo = Integer.parseInt(localPort);
			byte[] buffer = new byte[MAX_SIZE];
			socket = new DatagramSocket(portNo);
			pkt = new DatagramPacket(buffer, buffer.length);
			
			// Receive file name and size from sender respectively
			socket.receive(pkt);
			String fileName = new String(pkt.getData(), 0, pkt.getLength());
			socket.receive(pkt);
			String fileSize = new String(pkt.getData(), 0, pkt.getLength());

			// Receive file from sender as long as file size is <= 1000 bytes
			FileOutputStream fos = new FileOutputStream(fileName, true);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			int fileSizeBytes = Integer.parseInt(fileSize);
			int numBytes = 0;

			while(numBytes < fileSizeBytes) {
				socket.receive(pkt);
				bos.write(pkt.getData(), 0, pkt.getLength());
				numBytes += pkt.getLength();
			}

			bos.close();
			socket.close();

			System.out.println("File is received successfully.");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
}