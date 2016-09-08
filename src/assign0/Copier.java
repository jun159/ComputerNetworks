package assign0;

//Name: Luah Bao Jun (A0126258A)
//Assignment 0 Exercise 4

import java.io.*;

public class Copier {

	private static final String MESSAGE = "%1$s is successfully copied to %2$s";

	private String source, destination;

	public Copier() {

	}

	public Copier(String source, String destination) {
		this.source = source;
		this.destination = destination;
	}

	private void run() {
		try {
			FileInputStream fis = new FileInputStream(source);
			FileOutputStream fos = new FileOutputStream(destination);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			byte[] buffer = new byte[1000];
			int numBytes;

			while ((numBytes = bis.read(buffer)) > 0) {
				bos.write(buffer, 0, numBytes);
			}

			bis.close();
			bos.close();
		} catch (FileNotFoundException e) {
			e.getMessage();
		} catch (IOException e) {
			e.getMessage();
		}

		System.out.println(String.format(MESSAGE, source, destination));
	}

	public static void main(String[] args) {
		Copier copier = new Copier(args[0], args[1]);
		copier.run();
	}
}