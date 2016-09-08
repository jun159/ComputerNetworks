package assign0;

//Name: Luah Bao Jun (A0126258A)
//Assignment 0 Exercise 3

import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.zip.CRC32;

public class Checksum {

	private String pathName;

	public Checksum() {

	}

	public Checksum(String pathName) {
		this.pathName = pathName;
	}

	private void run() {

		try {
			byte[] bytes = Files.readAllBytes(Paths.get(pathName));
			CRC32 crc = new CRC32();
			crc.update(bytes);
			System.out.println(crc.getValue());
		} catch (IOException e) {
			e.getMessage();
		}
	}

	public static void main(String[] args) {
		Checksum checksum = new Checksum(args[0]);
		checksum.run();
	}

}