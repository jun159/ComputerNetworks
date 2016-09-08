package assign3;

//Author: Luah Bao Jun
//Assignment 3

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import java.net.*;

/**********************************************************************
* This skeleton program is prepared for weak and average students.  *
*                                                                   *
* If you are very strong in programming, DIY!                       *
*                                                                   *
* Feel free to modify this program.                                 *
*********************************************************************/

//Alice knows Bob's public key
//Alice sends Bob session (AES) key
//Alice receives messages from Bob, decrypts and saves them to file

class Alice {  // Alice is a TCP client
 
 private ObjectOutputStream toBob;   // to send session key to Bob
 private ObjectInputStream fromBob;  // to read encrypted messages from Bob
 private Crypto crypto;              // object for encryption and decryption

 private static final int SIZE_LINE = 100;
 private static final int SIZE_AES = 128;
 
 public static final String TYPE_AES = "AES";
 public static final String MESSAGE_FILE = "msgs.txt"; // file to store messages
 
 public static void main(String[] args) {
     
     // Check if the number of command line argument is 2
     if (args.length != 2) {
         System.err.println("Usage: java Alice BobIP BobPort");
         System.exit(1);
     }
     
     new Alice(args[0], args[1]);
 }
 
 // Constructor
 public Alice(String ipStr, String portStr) {
     
     // Get Bob's public key and generates session key using Bob's public key to encrypt 
     // Session key is for both parties to use for sending and receiving the message
     this.crypto = new Crypto();

     int port = Integer.parseInt(portStr);

     Socket clientSkt = null;   // socket used to talk to Bob
     
     try {
         InetAddress ip = InetAddress.getByName(ipStr);
         clientSkt = new Socket(ip, port);
     } catch (IOException ioe) {
         System.out.println("Error creating client socket");
         System.exit(1);
     }

     try {
         // Send session key to Bob
         this.toBob = new ObjectOutputStream(clientSkt.getOutputStream());   
         // Received encrypted message from Bob
         this.fromBob = new ObjectInputStream(clientSkt.getInputStream());   
     } catch (IOException ioe) {
         System.out.println("Error: cannot get input/output streams");
         System.exit(1);
     }

     // Send session key to Bob
     sendSessionKey();
     
     // Receive encrypted messages from Bob,
     // decrypt and save them to file
     receiveMessages();

     // Clean up
     try {
         clientSkt.close();
     } catch (IOException ioe) {
         System.out.println("Error closing TCP sockets");
         System.exit(1);
     }
 }
 
 // Send session key to Bob
 public void sendSessionKey() {
     
      try {
         SealedObject sessionKeyObj = this.crypto.getSessionKey();
         this.toBob.writeObject(sessionKeyObj);

         System.out.println("Successful: sent session key to Bob");
     } catch (IOException ioe) {
         System.out.println("Error sending session key to Bob");
         System.exit(1);
     } 
 }
 
 // Receive messages one by one from Bob, decrypt and write to file
 public void receiveMessages() {
     
     try {
         File file = new File(MESSAGE_FILE);
         PrintWriter fileWriter = new PrintWriter(file);

         for(int i = 0; i < SIZE_LINE; i++) {
             SealedObject encryptedMsg = (SealedObject) this.fromBob.readObject();
             String message = this.crypto.decryptMsg(encryptedMsg);
             fileWriter.write(message + "\n");
         }

         fileWriter.close();
         System.out.println("All messages are received from Bob");
     
     } catch (FileNotFoundException fnfe) {
         System.out.println("Error: " + MESSAGE_FILE + " doesn't exist");
         System.exit(1);
     } catch (ClassNotFoundException ioe) {
         System.out.println("Error: cannot typecast to class SealedObject");
         System.exit(1); 
     } catch (IOException ioe) {
         System.out.println("Error sending messages to Alice");
         System.exit(1);
     }
 }
 
 /*****************/
 /** inner class **/
 /*****************/
 class Crypto {
     
     // Bob's public key, to be read from file
     private PublicKey pubKey;
     // Alice generates a new session key for each communication session
     private SecretKey sessionKey;
     // File that contains Bob' public key
     public static final String PUBLIC_KEY_FILE = "public.key";
     
     // Constructor
     public Crypto() {
         // Read Bob's public key from file
         readPublicKey();
         // Generate session key dynamically
         initSessionKey();
     }
     
     // Read Bob's public key from file
     public void readPublicKey() {
         // key is stored as an object and need to be read using ObjectInputStream.
         // See how Bob read his private key as an example.
        try {
             ObjectInputStream ois = 
                 new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
             this.pubKey = (PublicKey)ois.readObject();
             ois.close();

             System.out.println("Successful: read bob's public key");
         } catch (IOException oie) {
             System.out.println("Error reading public key from file");
             System.exit(1);
         } catch (ClassNotFoundException cnfe) {
             System.out.println("Error: cannot typecast to class PublicKey");
             System.exit(1);            
         }

         System.out.println("Public key read from file " + PUBLIC_KEY_FILE);
     }
     
     // Generate a session key
     public void initSessionKey() {
         // suggested AES key length is 128 bits
         try {
             KeyGenerator key = KeyGenerator.getInstance(TYPE_AES);
             key.init(SIZE_AES);
             this.sessionKey = key.generateKey();

             System.out.println("Successful: generated session key");

         } catch (Exception e) {
             System.out.println("Error: unable to generate session key");
             System.exit(1);   
         }
     }
     
     // Seal session key with RSA public key in a SealedObject and return
     public SealedObject getSessionKey() {

         SealedObject sessionKeyObj = null;
         
         try {
             // Alice must use the same RSA key/transformation as Bob specified
             Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
             cipher.init(Cipher.ENCRYPT_MODE, this.pubKey);
             // RSA imposes size restriction on the object being encrypted (117 bytes).
             // Instead of sealing a Key object which is way over the size restriction,
             // we shall encrypt AES key in its byte format (using getEncoded() method).  
             byte[] rawkey = this.sessionKey.getEncoded();
             sessionKeyObj = new SealedObject(rawkey, cipher);
         } catch (GeneralSecurityException gse) {
             System.out.println("Error: wrong cipher to encrypt session key");
             System.exit(1);
         } catch (IOException ioe) {
             System.out.println("Error sealing session key");
             System.exit(1);
         }

         return sessionKeyObj;
     }
     
     // Decrypt and extract a message from SealedObject
     public String decryptMsg(SealedObject encryptedMsgObj) {
         
         String plainText = null;
         
         // Alice and Bob use the same AES key/transformation
         try {
             Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
             cipher.init(Cipher.DECRYPT_MODE, this.sessionKey);
             plainText = (String) encryptedMsgObj.getObject(cipher);
         } catch (GeneralSecurityException gse) {
             System.out.println("Error: wrong cipher to decrypt message");
             System.exit(1);
         } catch (ClassNotFoundException ioe) {
             System.out.println("Error: cannot typecast to byte array");
             System.exit(1); 
         } catch (IOException ioe) {
             System.out.println("Error creating SealedObject");
             System.exit(1);
         }
         
         return plainText;
     }
 }
}