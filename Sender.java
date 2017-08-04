import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Date;
import java.util.Scanner;

// The sender class
public class Sender {
    static DatagramSocket senderSocket; // Declare the Sender Transmission Packet
    static InetAddress receiver_IPAddress; // Stores the Receivers IP Address in InetAddress form
    static byte[] data_to_send; // Will contain the dummy data to send
    static int max_packet_size = 1000; // Defines the MSS (default to 1000)
    static List<Integer> packet_seq; // A list of serial numbers of the packet in transmission
    static List<Long> timer_values; // A list of timers for each packet in transmission
    static int total_data_size = 100000; // The total size of the dummy date to be sent (default to 100000)
    static int LastAck = 0; // Keeps track of the cumulative ACK (Initially 0)
    static String receiver_ip="127.0.0.1"; // Set default ip address to localhost
    static int loss = 0; // Set dafault l flag value to 0
    static int port_no = 9876; // Set default port number to 9876
    static int timeout = 1000; // Set default timeout to 1000 ms

    // Function to see if any timer in our timer list has timed out
    public static boolean timer_expired() {
        Date dNew = new Date();
        long final_time = dNew.getTime(); // Gets current user time
        for(int i = 0; i < packet_seq.size(); i++) {
            if(Math.abs(timer_values.get(i)-final_time) > 1000) { // Checks to see if timeout has occured
                System.out.println("Timer expired for seq_num"+packet_seq.get(i));
                return true;
            }
        }
        return false;
    }

    // Function that sends a packet to receiver with specific sequence number
    public static void send_packet(int seq_num) {
        if(seq_num == -1) { // Sequence number must be non-negative
            seq_num = 0;
        }
        if(seq_num >= total_data_size) { // Send only data from our dummy data
            return;
        }
            try {
                // Construct a byte stream to encode sequence number
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                DataOutputStream daos=new DataOutputStream(baos);
                daos.writeInt(seq_num);
                // Add dummy bytes to our packet
                byte[] payload_bytes = new byte[max_packet_size];
                System.out.println("Copying from seq_num and Sending:"+seq_num);
                System.arraycopy( data_to_send, seq_num, payload_bytes, 0, max_packet_size ); // Get bytes from dummy data
                daos.write(payload_bytes);
                byte[] bytes_to_send = baos.toByteArray(); // Convert all data into a byte stream
                // Create a datagram packet with the payload
                DatagramPacket sendPacket = new DatagramPacket(bytes_to_send, bytes_to_send.length, receiver_IPAddress, port_no);
                
                if((Math.random()>=0.05 && loss == 1) || loss == 0) { // Check to see whether or not to loose packet
                    // Send Packet
                    senderSocket.send(sendPacket);
                }
                else {
                    // Don't Send Packet
                    System.out.println("Dropped Packet Here!! with seq_num :"+seq_num);
                }
                Date dNow = new Date();
                long packet_time=dNow.getTime();
                timer_values.add(packet_time); // Start timer for packet
                packet_seq.add(seq_num); // Add packet to packet list
            }
            catch (IOException ex) {
                System.err.println(ex);
            }
    }

    // Function that deletes the timser data for all those packets for which Acknowledgement has been received
    public static void delete_timers() {
        for(int i = 0; i < packet_seq.size(); i++) {
            if(packet_seq.get(i) < LastAck) {
                timer_values.remove(i);
                packet_seq.remove(i);
            }
        }
    }

    // This function indicates the way to execute the code
    static void usage() {
        System.out.println("usage: client IP Port -l P (-h)");
        System.out.println("       IP = IP Address");
        System.out.println("       Port = Port Number");
        System.out.println("       -l = l flag");
        System.out.println("       P = flag value");
        System.out.println("       -h = to see help");
    }

    // Main function
    public static void main(String args[]) throws Exception {
        // check for help input
        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        // read passed parameters
        if (args.length >= 2) {
            // Read input IP Address of receiver
            try {
                receiver_ip = args[0];
            } catch (IllegalArgumentException e) {
                System.err.println("Argument 'IP Address' must be in the proper format.");
                usage();
                System.exit(1);
            }
            // Read Port No for transmission to receiver
            try {
                port_no = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Argument 'port_no' must be an integer.");
                usage();
                System.exit(1);
            }
            // Read the l flag parameter
            try{
                if(args[2].equals("-l")) {
                    loss = Integer.parseInt(args[3]);
                }
                else{
                    usage();
                    System.exit(1);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }

        packet_seq = new ArrayList<Integer>(); // Declare Packet List
        timer_values = new ArrayList<Long>(); // Declare Timer List
        senderSocket = new DatagramSocket(); // Declare Sender Socket
        // Stuff dummy data to be sent
        String dummy = "";
        for(int i = 0; i < total_data_size; i++) {
            dummy+="a";
        }
        data_to_send = dummy.getBytes("UTF-8");
        System.out.println("Total Data Size in Bytes: " + data_to_send.length);
        System.out.println("Reciever IP is " + receiver_ip);
        receiver_IPAddress = InetAddress.getByName(receiver_ip); // Set receiver_IPAddress
        
        Date dNow = new Date();
        long initial_time = dNow.getTime(); // Get current time     
        int window_to_send = 1000; // Define Window size (Initially 1000)
        send_packet(LastAck); // Attempt to send initial packet
        byte[] receiveData = new byte[1300];
        // Send all packets in dummy data
        while( LastAck < (total_data_size- max_packet_size)) {
            try  {
                // Declare RecievePacket to store acknowledgement
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                System.out.println ("Waiting for datagram packet");
                senderSocket.setSoTimeout(100); // Set timeout to 100 ms
                senderSocket.receive(receivePacket);

                // Declare a byte stream to obtain data from receivePacket
                ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData());
                DataInputStream dais = new DataInputStream(bais);
                LastAck = dais.readInt();
                System.out.println("Recieved Ack with LastAck:" + LastAck);

                delete_timers(); // Update timers list
                // If a timer has expired attempt to send failed packet
                if(timer_expired()) {
                    window_to_send = max_packet_size; // reset window size
                    timer_values.clear();
                    packet_seq.clear();
                    send_packet(LastAck);
                }
                else { // Indicates Success
                    window_to_send += (max_packet_size*max_packet_size/window_to_send); // Update window size
                    // Send next packets in sequence
                    for(int i = LastAck; (i+max_packet_size-LastAck) < window_to_send;) {
                        if(!packet_seq.contains(i)) {
                            send_packet(i);
                        }
                        i += max_packet_size;
                    }
                }
                System.out.println("Window Size is "+window_to_send);
            }
            catch (SocketTimeoutException ste) { // Acknowledgement not received
                delete_timers(); // Update timers list
                // Attempt to send failed packet
                if(timer_expired()) {
                    window_to_send = max_packet_size;
                    timer_values.clear();
                    packet_seq.clear();
                    send_packet(LastAck);
                }
                System.out.println ("Timeout Occurred: Packet assumed lost");
                // break;
            }
        }
        // Close the socket
        senderSocket.close();
    }
}
