import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.OutputStream;
import java.util.Hashtable;
import java.lang.String;
import java.lang.Byte;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Random;



public final class UdpClient {

    public static void main(String[] args) throws Exception {
		
		
		//connecting to socket and setup io
        try (Socket socket = new Socket("codebank.xyz", 38005)) {
			System.out.println("\nConnected to server.");
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			
			
			
			//create and send handshake bytes
			byte[] handshake = createIPv4handShake();
			
			
			//send handshake
			os.write(handshake);
			
			
			//get and print out handshake response
			System.out.println("\nHandshake Response: " + Integer.toHexString(is.read()) + Integer.toHexString(is.read()) + Integer.toHexString(is.read()) + Integer.toHexString(is.read()));
			
			
			//receive, calculate, and print port number
			int port = (is.read() << 8);
			port += is.read();
			System.out.println("Port Number Received: " + port + "\n");
			
			
			//initialize the length of the data
			int dataLength = 1;
			
			
			//sending the 12 packets of incrementing size
			for(int i = 1; i < 13; i++) {
				//increment data size and print out
				dataLength *= 2;
				System.out.println("Sending packet with " + dataLength + " bytes of data");
				
				//get packets to send
				byte[] message = createIPv4UDP(dataLength, port);
				
				//record start time
				long startTime = System.currentTimeMillis();
				
				//send UDP/IPv4 packet
				os.write(message);
				
				//receive and print out response
				System.out.println("Response: " + Integer.toHexString(is.read()) + Integer.toHexString(is.read()) + Integer.toHexString(is.read()) + Integer.toHexString(is.read()));
				
				//get end time and calculate,print RTT
				long endTime = System.currentTimeMillis();
				long RTT = endTime - startTime;
				System.out.println("RTT: " + RTT + "ms" + "\n");
			}
			
			
		}
	}
	
	
	
	//checksum methods takes in a byte array and returns the checksum as a short
	public static short checksum(byte[] b) {
		long sum = 0;
		int count = 20;
		long byteComb;
		int i = 0;
		while(count > 1) {
			byteComb = (((b[i] << 8) & 0xFF00) | ((b[i + 1]) & 0xFF));
			sum += byteComb;
			if((sum & 0xFFFF0000) > 0 ) {
				sum &= 0xFFFF;
				sum += 1;
			}
			i += 2;
			count -= 2;
		}
		if(count > 0) {
			sum += (b[b.length-1] << 8 & 0xFF00);
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}
		sum = ~sum;
		sum = sum & 0xFFFF;
		return (short)sum;
	}
	
	
	
	//creates handshake IPv4 packet
	public static byte[] createIPv4handShake() {
		byte[] message = new byte[24];
		message[0] = (byte)69;
		message[1] = (byte)0;
		//size
		message[2] = (byte)0;
		message[3] = (byte)24;
		//size
		message[4] = (byte)0;
		message[5] = (byte)0;
		message[6] = (byte)64;
		message[7] = (byte)0;
		message[8] = (byte)50;
		message[9] = (byte)17;
		message[10] = (byte)0;
		message[11] = (byte)0;
		message[12] = (byte)134;
		message[13] = (byte)71;
		message[14] = (byte)249;
		message[15] = (byte)228;
		message[16] = (byte)52;
		message[17] = (byte)37;
		message[18] = (byte)88;
		message[19] = (byte)154;
		message[20] = (byte)222;
		message[21] = (byte)173;
		message[22] = (byte)190;
		message[23] = (byte)239;
		int checksum = (int)checksum(message);
		checksum = checksum & 0x0000FFFF;
		message[10] = (byte)(checksum >> 8);
		message[11] = (byte)checksum;
		return message;
	}
	
	//creates IPv4/UDP packet
	public static byte[] createIPv4UDP(int size, int destPort) {
		byte[] message = new byte[28 + size];
		message[0] = (byte)69;
		message[1] = (byte)0;
		//size
		message[2] = (byte)((28 + size) >> 8);
		message[3] = (byte)(28 + size);
		//size
		message[4] = (byte)0;
		message[5] = (byte)0;
		message[6] = (byte)64;
		message[7] = (byte)0;
		message[8] = (byte)50;
		message[9] = (byte)17;
		message[10] = (byte)0;
		message[11] = (byte)0;
		message[12] = (byte)134;
		message[13] = (byte)71;
		message[14] = (byte)249;
		message[15] = (byte)228;
		message[16] = (byte)52;
		message[17] = (byte)37;
		message[18] = (byte)88;
		message[19] = (byte)154;
		message[20] = (byte)0;
		message[21] = (byte)0;
		message[22] = (byte)(destPort >> 8);
		message[23] = (byte)destPort;
		message[24] = (byte)((size + 8) >> 8);
		message[25] = (byte)(size + 8);
		//UDPchecksum
		message[26] = (byte)0;
		message[27] = (byte)0;
		//UDPchecksum
		for(int i = 28; i < message.length; i++) {
			message[i] = randByte();
		}
		
		//IP checksum
		int checksum = (int)checksum(message);
		checksum = checksum & 0x0000FFFF;
		message[10] = (byte)(checksum >> 8);
		message[11] = (byte)checksum;
		
		//UDP checksum
		int UDPchecksum = (int)getUDPchecksum(message, size);
		UDPchecksum = UDPchecksum & 0x0000FFFF;
		message[26] = (byte)(UDPchecksum >> 8);
		message[27] = (byte)UDPchecksum;
		

		return message;
	}
	
	
	
	public static byte randByte() {
		Random rand = new Random();
		return (byte)rand.nextInt(256);
	}
	
	
	
	//gets UDP packet checksum
	public static short getUDPchecksum(byte[] message, int dataLength) {
		
		
		//create psuedo header
		byte[] psuedoHeader = new byte[20 + dataLength];
		psuedoHeader[0] = message[12];
		psuedoHeader[1] = message[13];
		psuedoHeader[2] = message[14];
		psuedoHeader[3] = message[15];
		psuedoHeader[4] = message[16];
		psuedoHeader[5] = message[17];
		psuedoHeader[6] = message[18];
		psuedoHeader[7] = message[19];
		psuedoHeader[8] = (byte)0;
		psuedoHeader[9] = message[9];
		psuedoHeader[10] = message[24];
		psuedoHeader[11] = message[25];
		psuedoHeader[12] = message[20];
		psuedoHeader[13] = message[21];
		psuedoHeader[14] = message[22];
		psuedoHeader[15] = message[23];
		psuedoHeader[16] = message[24];
		psuedoHeader[17] = message[25];
		psuedoHeader[18] = message[26];
		psuedoHeader[19] = message[27];
		for(int i = 0; i < dataLength; i++) {
			psuedoHeader[20 + i] = message[28 + i];
		}
		
		
		
		//calculate the checksum using the psuedo header
		long sum = 0;
		int count = psuedoHeader.length;
		long byteComb;
		int i = 0;
		while(count > 1) {
			byteComb = (((psuedoHeader[i] << 8) & 0xFF00) | ((psuedoHeader[i + 1]) & 0xFF));
			sum += byteComb;
			if((sum & 0xFFFF0000) > 0 ) {
				sum &= 0xFFFF;
				sum += 1;
			}
			i += 2;
			count -= 2;
		}
		if(count > 0) {
			sum += (psuedoHeader[psuedoHeader.length-1] << 8 & 0xFF00);
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}
		sum = ~sum;
		sum = sum & 0xFFFF;
		return (short)sum;
	}
	
}