import java.io.*;
import java.net.*;
import java.util.*;

// The receiver class
class Receiver {

    // This function indicates the way to execute the code
    static void usage() {
        System.out.println("usage: client Port (-h)");
        System.out.println("       Port = Port Number");
        System.out.println("       -h = to see help");
    }

    // Main function
    public static void main(String args[]) throws Exception {
        int port_no = 9876; // Set default port_no

        // check for help input
        if (args.length < 1){
            usage();
            System.exit(1);
        }

        // read passed parameters
        if (args.length == 1) {
            // Read the port number
            try {
                port_no = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Argument 'port_no' must be an integer.");
                usage();
                System.exit(1);
            }
            try{
                if(args[0].equals("-h")){
                    System.err.println("User asked for help");
                    usage();
                    System.exit(1);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("See usage");
            }
        }
        else {
            System.out.println ("Running with default PORT = 9876");
        }

        int LastAck = 0; // Set the cumulative ACK to 0
        List<Integer> al = new ArrayList<Integer>(); // Create a list of sequence numbers
        try {
            // Create the Receiving Socket
            DatagramSocket recieverSocket = new DatagramSocket(port_no);

            byte[] receiveData = new byte[1300];
            byte[] sendData = new byte[1300];

            // Listen Indefinetely
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // Declare packet
                System.out.println ("Waiting for datagram packet");
                recieverSocket.receive(receivePacket); // Receive Packet

                // Declare a byte stream to obtain data from receivePacket
                ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData());
                DataInputStream dais = new DataInputStream(bais);
                int seq_num = dais.readInt();
                al.add(seq_num); // Add sequence number to list
                int PACKET_SIZE = receivePacket.getLength();
                System.out.println("Recieved Packet size is "+ PACKET_SIZE);
                System.out.println("Sequence number is "+ seq_num);
                Collections.sort(al); // Sort list according to sequence number
                for(int i = 0; i < 100000;) {
                    if(!al.contains(i)) { // Update LaskAck
                        LastAck = i; 
                        break;
                    }
                    i += 1000;
                }
                InetAddress IPAddress = receivePacket.getAddress(); // Get Sender's IpAddress
                int port = receivePacket.getPort();
                System.out.println ("From: " + IPAddress + ":" + port);

                // Construct a byte stream to encode LastAck
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                DataOutputStream daos=new DataOutputStream(baos);
                daos.writeInt(LastAck);
                sendData=baos.toByteArray();
                System.out.println("Send Ack with Last Ack "+ LastAck);
                // Create a send packet
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,port);
                recieverSocket.send(sendPacket); // Send Packet
            }
        }
        catch (SocketException ex) {
            System.out.println("UDP Port 9876 is occupied.");
            System.exit(1);
        }
    }
}
