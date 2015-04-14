package flashcast;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by Jake on 2015-02-02.
 */
public class CastListActivity
{;
    private PacketFunctions pb;
    private ServerSocket server;
    private Thread serverThread = null;
    private Thread broadCast = null;
    private Thread broadCastServer = null;
    public Settings prefs;

    public void start(Registry reg)
    {
        pb = new PacketFunctions();
        prefs = new Settings();

        broadCast = new Thread(new broadCastThread());
        broadCastServer = new Thread(new broadCastServerThread());
        serverThread = new Thread(new ServerThread());
        
        broadCast.start();
        broadCastServer.start();
        serverThread.start();
    }

    private class broadCastServerThread implements Runnable
    {
        @Override
        public void run()
        {
            byte[] messageArray = new byte[Constants.BROADCASTSIZE/2];
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket(Constants.BROADCASTPORT, InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(messageArray, messageArray.length);
                while (true)
                {
                    socket.receive(packet);
                    String ipAddress = packet.getAddress().toString();

                    //if the packet is from you, ignore it
                    if (!ipAddress.contains(getIPAddress()))
                    {
                        BroadCastPacket temp = pb.parseBroadcastPacket(messageArray);

                        if (temp.getTYPE() == Constants.REGISTER)
                        {
                            register(temp, ipAddress);
                        }
                    }
                }
            } catch (SocketException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
        }
    }
    
     private class broadCastThread implements Runnable
    {
        @Override
        public void run()
        {
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                byte[] messageArray = new byte[Constants.BROADCASTSIZE];
                byte[] temp = pb.buildHelloPacket();
                int counter = 0;

                while (true)
                {
                    messageArray = temp;
                    socket.send(new DatagramPacket(messageArray, messageArray.length, InetAddress.getByName(getBroadcastAddress()), Constants.BROADCASTPORT));
                    Thread.sleep(5000);
                    if (counter++ > 3)
                    {
                        counter = 0;
                        Globals.register = new Registry();
                        System.out.println("unregister all");
                    }
                }
            } catch (SocketException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            } finally
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
        }
    }
    
    public void register(BroadCastPacket temp, String ipAddress)
    {
        ipAddress = ipAddress.substring(1);

        switch (temp.getCODE())
        {
            case Constants.HELLO_E:
                if (temp.getSEQ() == 0)
                {
                    if (!Globals.register.checkRegister(ipAddress))
                    {
                        Globals.register.addRegister(ipAddress, temp.getDataString());
                        try
                        {
                            DatagramSocket sock = new DatagramSocket();
                            byte[] messageArray = new byte[Constants.BROADCASTSIZE];
                            messageArray = pb.buildResponsePacket();
                            sock.send(new DatagramPacket(messageArray, messageArray.length, InetAddress.getByName(ipAddress), Constants.BROADCASTPORT));
                        } catch (SocketException e)
                        {
                            e.printStackTrace();
                        } catch (UnknownHostException e)
                        {
                            e.printStackTrace();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case Constants.HELLO_R:
                Globals.register.addRegister(ipAddress, temp.getDataString());
                break;
            case Constants.GOODBYE:
                Globals.register.removeRegister(ipAddress, temp.getDataString());
                break;
        }
    }
    
    public String getIPAddress() 
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) 
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) 
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {}
        return null;
    }

    public String getBroadcastAddress() throws SocketException 
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) 
            {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) 
                {
                    if (interfaceAddress.getAddress() instanceof Inet4Address)
                    {
                        return interfaceAddress.getBroadcast().toString().substring(1);
                    }
                }
            }
        }
        return null;
    }
    
    private class ServerThread implements Runnable
    {
        @Override
        public void run()
        {
            Socket socket = null;
            try
            {
                server = new ServerSocket(Constants.TCPPORT);
                while (!Thread.currentThread().isInterrupted())
                {
                    socket = server.accept();
                    if (Globals.register.checkRegister(socket.getInetAddress().getHostAddress()))
                    {
                        TransferServerThread tThread = new TransferServerThread(socket);
                        new Thread(tThread).start();
                    } else {
                        socket.close();
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class TransferServerThread implements Runnable
    {
        private Socket client;
        private InputStream input;
        private OutputStream output;
        private int counter = 0;
        private boolean isFile;
        private Long fileSize;
        private String fileName;

        public TransferServerThread(Socket client)
        {
            this.client = client;
            try
            {
                this.input = this.client.getInputStream();
                this.output = this.client.getOutputStream();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            byte[] buff = new byte[Constants.CIPHERSIZE];
            Arrays.fill(buff, (byte) 0);
            try
            {
                String pubKey = Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString());
                while (this.input.read(buff, 0, buff.length) > -1) // AWAIT SEND REQUEST
                {
                    Packet request = pb.parsePacket(buff);
                    if (request.getSEQ() == counter%10 && request.getTYPE() == Constants.DIRECTION && request.getCODE() == Constants.SENDINGREQ) //Checking send request
                    {
                        fileName = request.getDataString();
                        String temp[] = new String[2];
                        if (fileName.contains("http://"))
                        {
                            fileName = fileName.split("http://")[1];
                        }
                        
                        if (fileName.contains(":"))
                        {
                            isFile = true;
                            temp = fileName.split(":");
                            fileName = temp[0];
                            fileSize = Long.parseLong(temp[1]);
                        }
                        else
                        {
                            fileSize = Long.parseLong(fileName);
                            fileName = "";
                        }

                        counter++;
                        byte[] sendResponse;
                        System.out.println(prefs.getFirewallState());
                        if (prefs.getFirewallState())
                        {
                            if (isFile = !fileName.isEmpty())
                            {
                                sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), prefs.getFileState());
                            } else 
                            {
                                sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), prefs.getLinkState());
                            }
                        } else 
                        {
                            sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), true);
                        }
                        
                        this.output.write(sendResponse);
                        counter++;
                        break;
                    }
                    else
                    {
                        counter++;
                        System.out.println("Request recieved DENY");
                        byte[] sendResponse = pb.buildSendResponsePacket(pubKey, false);
                        this.output.write(sendResponse); // SEND REQUEST response
                        counter++;
                        return;
                    }
                }

                Arrays.fill(buff, (byte) 0);

                while (true)
                {
                    int numberPackets = 0;
                    int remainder = 0;
                    int finalSize = 0;
                    int currentSize = 0;
                    int readSize = 0;
                    byte[] packets;
                    
                    numberPackets = fileSize.intValue() / (Constants.MAXSIZE-7);
                    remainder = fileSize.intValue() % (Constants.MAXSIZE-7);
                    if (remainder > 0)
                    {
                        numberPackets++;
                    }

                    finalSize = numberPackets * Constants.CIPHERSIZE;

                    packets = new byte[finalSize+1];
                    ByteBuffer wrapper = ByteBuffer.wrap(packets);

                    while (finalSize != currentSize)
                    {
                        if ((readSize = this.input.read(buff, 0, buff.length)) > -1)
                        {
                            wrapper.put(buff);
                            currentSize += readSize;
                        }
                    }
                    counter += numberPackets;

                    Arrays.fill(buff, (byte) 0);

                    while (this.input.read(buff, 0, buff.length) > -1)
                    {
                        Packet response = pb.parsePacket(buff);
                        if (response.getSEQ() == counter%10 && response.getTYPE() == Constants.DIRECTION && response.getCODE() == Constants.FINISHED)
                        {
                            // read finished packet
                            counter++;
                            System.out.println("FINISHED");
                            break;
                        }
                    }

                    if (isFile)
                    {
                        boolean accept = prefs.getFileState();
                        
                        if (!prefs.getFirewallState())
                        {
                            accept = true;
                        }
                        
                        if (accept)
                        {                           
                            String data = pb.transferPacketParser(packets, numberPackets);
                            File f = new File(fileName);
                            FileOutputStream out = new FileOutputStream(f);
                            out.write(data.getBytes());
                            out.flush();
                            out.close();
                        }
                        
                        System.out.println(accept);
                        byte[] transferResponse = pb.buildTransferResponsePacket(pubKey, accept, counter);
                        this.output.write(transferResponse); // SEND TRANSFER RESPONSE
                        this.output.flush();
                        counter++;
                    } else
                    {
                        // prompt user, if they're into that sort of thing
                        String url = pb.transferPacketParser(packets, numberPackets);
                        System.out.println(url);
                        
                        if (!url.contains("http://") && !url.contains("https://"))
                        {
                            url = "http://" + url;
                        } 
                                 
                        boolean accept = prefs.validateURL(url);
                        
                        byte[] transferResponse = pb.buildTransferResponsePacket(pubKey, accept, counter);
                        this.output.write(transferResponse); // SEND TRANSFER RESPONSE
                        this.output.flush();
                        counter++;
                        
                        if (accept)
                        {                          
                            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) 
                            {    
                                try 
                                {
                                    desktop.browse(new URI(url));
                                } catch (Exception e) 
                                {
                                    e.printStackTrace();
                                }
                            } else 
                            {
                                Runtime runtime = Runtime.getRuntime();
                                try {
                                    runtime.exec("xdg-open " + url);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}
