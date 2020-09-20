/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapplication_server.components.ClientSocketEngine;

import SocketActionMessages.ChatMessage;
import chatapplication_server.components.ConfigManager;
import chatapplication_server.encryption.AES;
import chatapplication_server.encryption.DiffieHellman;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import java.net.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author atgianne
 */
public class P2PClient extends JFrame implements ActionListener 
{
    private String host;
    private String port;

    // Port used to send messages
    private int senderPort = -1;

    // Maps used to keep info about different addresses we have been communicating with
    private HashMap<String,String> addressForAddress = new HashMap<String, String>();
    private HashMap<String,String> secretForAddress = new HashMap<String, String>();
    private HashMap<String,Integer> pForAddress = new HashMap<String, Integer>();
    private HashMap<String,Integer> gForAddress = new HashMap<String, Integer>();
    private HashMap<String,Integer> aForAddress = new HashMap<String, Integer>();
    private HashMap<String,Integer> AForAddress = new HashMap<String, Integer>();
    private HashMap<String,Integer> bForAddress = new HashMap<String, Integer>();
    private HashMap<String,Integer> BForAddress = new HashMap<String, Integer>();

    // These regular expressions are used to parse messages for numbers
    Pattern receivePortMessagePattern = Pattern.compile(".*R_PORT=(\\d+).*");
    Pattern receivePortRequestPattern = Pattern.compile(".*R_PORT\\?.*");
    Pattern pMessagePattern = Pattern.compile(".*p=(\\d+).*");
    Pattern gMessagePattern = Pattern.compile(".*g=(\\d+).*");
    Pattern AMessagePattern = Pattern.compile(".*A=(\\d+).*");
    Pattern BMessagePattern = Pattern.compile(".*B=(\\d+).*");

    private final JTextField tfServer;
    private final JTextField tfPort;
    private final JTextField tfsPort;
    private final JLabel label;
    private final JTextField tf;
    private final JTextArea ta;
    protected boolean keepGoing;
    JButton send, start;
    
    P2PClient(){
        super("P2P Client Chat");
        host=ConfigManager.getInstance().getValue( "Server.Address" );
        port=ConfigManager.getInstance().getValue( "Server.PortNumber" );
        
        // The NorthPanel with:
        JPanel northPanel = new JPanel(new GridLayout(3,1));
        // the server name anmd the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);
        
        tfsPort=new JTextField(5);
        tfsPort.setHorizontalAlignment(SwingConstants.RIGHT);
        start=new JButton("Start");
        start.addActionListener(this);

        serverAndPort.add(new JLabel("Receiver's Port No:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel("Receiver's IP Add:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel(""));
        // adds the Server an port field to the GUI
        northPanel.add(serverAndPort);

        // the Label and the TextField
        label = new JLabel("Enter message below", SwingConstants.LEFT);
        northPanel.add(label);
        tf = new JTextField();
        tf.setBackground(Color.WHITE);
        northPanel.add(tf);
        add(northPanel, BorderLayout.NORTH);
        
        // The CenterPanel which is the chat room
        ta = new JTextArea(" ", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);

//        ta2 = new JTextArea(80,80);
//        ta2.setEditable(false);
//        centerPanel.add(new JScrollPane(ta2));   
        add(centerPanel, BorderLayout.CENTER);
        
        
        send = new JButton("Send");
        send.addActionListener(this);
        JPanel southPanel = new JPanel();
        southPanel.add(send);
        southPanel.add(start);
        JLabel lbl=new JLabel("Sender's Port No:");
        southPanel.add(lbl);
        tfsPort.setText("0");
        southPanel.add(tfsPort);
        add(southPanel, BorderLayout.SOUTH);
        
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

//        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tf.requestFocus();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if(o == send){
            if ( tfPort.getText().equals( ConfigManager.getInstance().getValue( "Server.PortNumber" ) ) )
            {
                display( "Cannot give the same port number as the Chat Application Server - Please give the port number of the peer client to communicate!\n" );
                return;
            }
            String receiveAddress = localhostUnifyName(tfServer.getText())+":"+tfPort.getText();
            if (this.secretForAddress.containsKey(receiveAddress)) {
                // Already have shared secret with recipient, encrypt...
                send(AES.encrypt(tf.getText(), secretForAddress.get(receiveAddress)));
                display("you: "+tf.getText());

            } else {
                // No shared secret with recipient
                if (keepGoing) {
                    // Port is open, we are ready to setup negotiation of a shared secret
                    send("R_PORT="+tfsPort.getText()+",R_PORT?");
                } else {
                    // Port is not open, we can't start negotiation of a shared secret
                    display("You need to open a port in order to negotiate a secret with recipient");
                }
            }
        }
        if(o == start){
            new ListenFromClient().start();
        }
    }
    
    public void display(String str) {
        ta.append(str + "\n");
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    
    public boolean send(String str){
        Socket socket = new Socket();
        ObjectOutputStream sOutput;		// to write on the socket
        // try to connect to the server
        try {
                socket.setSoLinger(true, 0);
                socket.setSoTimeout(0);

                if (senderPort != -1) {
                    socket.bind(new InetSocketAddress(senderPort));
                }
                socket.connect(new InetSocketAddress(tfServer.getText(), Integer.parseInt(tfPort.getText())));
                senderPort = socket.getLocalPort();

            }
            // if it failed not much I can so
            catch(Exception ec) {
                    display("Error connectiong to server:" + ec.getMessage() + "\n");
                    return false;
            }

            /* Creating both Data Stream */
            try
            {
//			sInput  = new ObjectInputStream(socket.getInputStream());
                    sOutput = new ObjectOutputStream(socket.getOutputStream());
            }
            catch (IOException eIO) {
                    display("Exception creating new Input/output Streams: " + eIO);
                    return false;
            }

        try {
            sOutput.writeObject(new ChatMessage(str.length(), str));
            display("You: " + str);
            sOutput.close();
            socket.close();
        } catch (IOException ex) {
            display("Exception creating new Input/output Streams: " + ex);
        }

         return true;
    }

    // Helper method to unify hostnames of localhost.
    // Sometimes it is referred to as localhost and sometimes as 127.0.0.1.
    private String localhostUnifyName(String hostname) {
        if (hostname.equals("localhost")) {
            return "127.0.0.1";
        }
        return hostname;
    }
    
    private class ListenFromClient extends Thread{
            public ListenFromClient() {
                keepGoing=true;
            }

            @Override
            public void run() {
                try 
		{ 
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(Integer.parseInt(tfsPort.getText()));
                        //display("Server is listening on port:"+tfsPort.getText());
                        ta.append("Server is listening on port:"+tfsPort.getText() + "\n");
                        ta.setCaretPosition(ta.getText().length() - 1);

			// infinite loop to wait for connections
			while(keepGoing) 
			{
                            // format message saying we are waiting

                            Socket socket = serverSocket.accept();  	// accept connection

                            ObjectInputStream sInput=null;		// to write on the socket

                            /* Creating both Data Stream */
                            try
                            {
                                    sInput = new ObjectInputStream(socket.getInputStream());
                            }
                            catch (IOException eIO) {
                                    display("Exception creating new Input/output Streams: " + eIO);
                            }

                            String msg = new String();
                            try {
                                msg = ((ChatMessage) sInput.readObject()).getMessage();
                                display(socket.getInetAddress()+":" + socket.getPort() + ": " + msg);

                                sInput.close();
                                socket.close();
                            } catch (IOException ex) {
                                display("Exception creating new Input/output Streams: " + ex);
                            } catch (ClassNotFoundException ex) {
                                Logger.getLogger(P2PClient.class.getName()).log(Level.SEVERE, null, ex);
                            }


                            String senderAddress = socket.getInetAddress()+":"+socket.getPort();
                            if (addressForAddress.containsKey(senderAddress)) {
                                String receiveAddress = addressForAddress.get(senderAddress);
                                if (secretForAddress.containsKey(receiveAddress)) {
                                    // Has a shared secret with this sender, decrypt...
                                    display(socket.getInetAddress()+":" + socket.getPort() + ": " + AES.decrypt(msg, secretForAddress.get(receiveAddress)));

                                } else {
                                    // We don't have a shared secret with this address
                                    Matcher pMessageMatcher = pMessagePattern.matcher(msg);
                                    Matcher gMessageMatcher = gMessagePattern.matcher(msg);
                                    if (pMessageMatcher.find() && gMessageMatcher.find()) {
                                        // Negotiation step 2
                                        // They sent us p and g
                                        pForAddress.put(receiveAddress, Integer.parseInt(pMessageMatcher.group(1)));
                                        gForAddress.put(receiveAddress, Integer.parseInt(gMessageMatcher.group(1)));
                                        // We get to pick a
                                        int a = DiffieHellman.pickA();
                                        int A = ((int) Math.pow(gForAddress.get(receiveAddress),a))% pForAddress.get(receiveAddress);
                                        aForAddress.put(receiveAddress, a);
                                        AForAddress.put(receiveAddress, A);
                                        send("A="+A);
                                    }

                                    Matcher AMessageMatcher = AMessagePattern.matcher(msg);
                                    if (AMessageMatcher.find()) {
                                        // Negotiation step 3
                                        // They sent us A
                                        AForAddress.put(receiveAddress, Integer.parseInt(AMessageMatcher.group(1)));
                                        // We get to pick b
                                        int b = DiffieHellman.pickB();
                                        int B = ((int) Math.pow(gForAddress.get(receiveAddress),b))% pForAddress.get(receiveAddress);
                                        bForAddress.put(receiveAddress, b);
                                        BForAddress.put(receiveAddress, B);
                                        send("B="+B);
                                        // We can calculate secret now
                                        int s = ((int) Math.pow(AForAddress.get(receiveAddress),b))% pForAddress.get(receiveAddress);
                                        String secret = Integer.toBinaryString(s);;
                                        secretForAddress.put(receiveAddress, secret);
                                        display("s = "+s);
                                        display("secret = "+secret);
                                    }

                                    Matcher BMessageMatcher = BMessagePattern.matcher(msg);
                                    if (BMessageMatcher.find()) {
                                        // Negotiation step 4
                                        // They send us B
                                        BForAddress.put(receiveAddress, Integer.parseInt(BMessageMatcher.group(1)));
                                        // We can calculate secret now
                                        int s = ((int) Math.pow(BForAddress.get(receiveAddress), aForAddress.get(receiveAddress)))% pForAddress.get(receiveAddress);
                                        String secret = Integer.toBinaryString(s);
                                        secretForAddress.put(receiveAddress, secret);
                                        display("s = "+s);
                                        display("secret = "+secret);
                                    }
                                }
                            } else {
                                // No receive address stored for this sender
                                Matcher receivePortMessageMatcher = receivePortMessagePattern.matcher(msg);
                                if (receivePortMessageMatcher.matches()) {
                                    // Receive address found in message
                                    String receiveAddress = socket.getInetAddress().toString().substring(1)+":"+receivePortMessageMatcher.group(1);
                                    addressForAddress.put(senderAddress, receiveAddress);
                                    addressForAddress.put(receiveAddress, senderAddress);

                                    Matcher receivePortRequestMatcher = receivePortRequestPattern.matcher(msg);
                                    if (receivePortRequestMatcher.find()) {
                                        // The sender also requests our receive address (to match it to our sender address)
                                        send("R_PORT="+tfsPort.getText());
                                    } else {
                                        // The sender did not request our receive address
                                        // Negotiation step 1
                                        // We get to pick p and g
                                        int p = DiffieHellman.pickP();
                                        int g = DiffieHellman.pickG(p);
                                        pForAddress.put(receiveAddress, p);
                                        gForAddress.put(receiveAddress, g);
                                        send("p="+p+",g="+g);
                                    }
                                }
                            }


                        }
		}
		// something went bad
		catch (IOException e) {
//            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
//			display(msg);
		}
	}		
    }
}
