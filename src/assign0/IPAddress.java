package assign0;

//Name: Luah Bao Jun (A0126258A)
//Assignment 0 Exercise 1

import java.util.*;

public class IPAddress {

	public IPAddress() {

	}

	private void run() {		
		Scanner sc = new Scanner(System.in);
		String binary = sc.next();
		ArrayList<String> ipArray = splitString(binary);

		System.out.println(ipArray.get(0) + "." 
				+ ipArray.get(1) + "."
				+ ipArray.get(2) + "."
				+ ipArray.get(3));
	}

	private ArrayList<String> splitString(String binary) {
		ArrayList<String> ipArray = new ArrayList<String>();

		for(int i = 0; i < binary.length(); i+=8) {
			ipArray.add(convertBinaryToDecimal(
						binary.substring(i,
							Math.min(i + 8, binary.length()))));
		}

		return ipArray;
	}

	private String convertBinaryToDecimal(String binary) {
		return String.valueOf(Integer.parseInt(binary, 2));	// base 2
	}

	public static void main(String[] args) {	
		IPAddress ipAddress = new IPAddress();			
		ipAddress.run();	
	}
}