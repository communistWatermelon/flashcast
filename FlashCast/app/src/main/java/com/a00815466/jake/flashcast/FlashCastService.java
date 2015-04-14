package com.a00815466.jake.flashcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Jake on 2015-03-16.
 */
public class FlashCastService extends IntentService
{
    private Thread sThread = null;
    private Thread registerThread;
    private Thread broadCastThread;
    private PacketFunctions pb;
    private Context context;
    private SharedPreferences prefs;

    public FlashCastService()
    {
        super("Download service");
    }

    public void startNotification(Intent intent)
    {
        if (intent.getAction() == null)
        {
            Globals.serviceRunning = true;
            Intent notificationIntent = new Intent(this, CastListActivity.class);
            notificationIntent.setAction(Constants.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent stopIntent = new Intent(this, FlashCastService.class);
            stopIntent.setAction(Constants.STOPSERVICE);
            PendingIntent pnextIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("FlashCast")
                    .setTicker("FlashCast")
                    .setContentText("Server Running")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_input_delete, "Stop Server", pnextIntent).build();
            startForeground(Constants.FOREGROUND_SERVICE, notification);
        } else if (intent.getAction().equals(Constants.STOPSERVICE))
        {
            Globals.serviceRunning = false;
            sThread.interrupt();
            registerThread.interrupt();
            broadCastThread.interrupt();

            sThread = null;
            registerThread = null;
            broadCastThread = null;

            stopForeground(true);
            stopSelf();
            System.exit(0);
        }
        Log.d("","");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID)
    {
        context = getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Globals.serviceRunning = true;
        pb = new PacketFunctions();

        sThread = new Thread(new ServerThread());
        sThread.start();

        startNotification(intent);
        startRegisterService();
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {

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
                this.client.setSoTimeout(Constants.TIMEOUT);
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

                while (this.input.read(buff, 0, buff.length) > -1 && !Thread.currentThread().isInterrupted()) // AWAIT SEND REQUEST
                {
                    Packet request = pb.parsePacket(buff, context);
                    if (request.getSEQ() == counter%10 && request.getTYPE() == Constants.DIRECTION && request.getCODE() == Constants.SENDINGREQ) //Checking send request
                    {
                        fileName = request.getDataString();
                        String temp[];
                        if (fileName.contains("http://"))
                        {
                            isFile = false;
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
                            isFile = false;
                            fileSize = Long.parseLong(fileName);
                            fileName = "";
                        }

                        counter++;
                        byte[] sendResponse;

                        if (prefs.getBoolean("firewall_state_preference", true))
                        {
                            // if it's a file
                            if(isFile)
                            {
                                sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), prefs.getBoolean("file_state_preference", true));
                            } else
                            {
                                sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), prefs.getBoolean("link_state_preference", true));
                            }
                            this.output.write(sendResponse); // SEND REQUEST response
                            counter++;
                        }
                        else
                        {
                            sendResponse = pb.buildSendResponsePacket(Globals.register.getKeyFromIP(this.client.getRemoteSocketAddress().toString()), true);
                            this.output.write(sendResponse);
                            counter++;
                        }
                        break;
                    }
                    else
                    {
                        counter++;
                        byte[] sendResponse = pb.buildSendResponsePacket(pubKey, false);
                        this.output.write(sendResponse); // SEND REQUEST response
                        counter++;
                        return;
                    }
                }

                Arrays.fill(buff, (byte) 0);

                while (!Thread.currentThread().isInterrupted())
                {
                    int currentSize = 0;
                    int numberPackets;
                    int remainder;
                    int finalSize;
                    int readSize;
                    byte[] packets;

                    numberPackets = fileSize.intValue() / (Constants.MAXSIZE-7);
                    remainder = fileSize.intValue() % (Constants.MAXSIZE-7);
                    if (remainder > 0)
                    {
                        numberPackets++;
                    }

                    finalSize = numberPackets * Constants.CIPHERSIZE;
//                    }

                    packets = new byte[finalSize];
                    ByteBuffer wrapper = ByteBuffer.wrap(packets);

                    while (finalSize != currentSize && !Thread.currentThread().isInterrupted())
                    {
                        if ((readSize = this.input.read(buff, 0, buff.length)) > -1)
                        {
                            wrapper.put(buff);
                            currentSize += readSize;
                        }
                    }
                    counter += numberPackets;

                    Arrays.fill(buff, (byte) 0);

                    while (this.input.read(buff, 0, buff.length) > -1 && !Thread.currentThread().isInterrupted())
                    {
                        Packet response = pb.parsePacket(buff, context);
                        if (response.getSEQ() == counter%10 && response.getTYPE() == Constants.DIRECTION && response.getCODE() == Constants.FINISHED)
                        {
                            // read finished packet
                            counter++;
                            Log.d("FINISHED", "");
                            break;
                        }
                    }

                    if (isFile)
                    {
                        String data = pb.transferPacketParser(packets, numberPackets, context);
                        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadDir.exists())
                        {
                            downloadDir.mkdirs();
                        }
                        FileOutputStream out = new FileOutputStream(new File(downloadDir, fileName));
                        out.write(data.getBytes());
                        out.flush();
                        out.close();
                    } else
                    {
                        String url = pb.transferPacketParser(packets, numberPackets, context);
                        if (url.contains("http://"))
                        {
                            url = url.replace("http://", "");
                        } else if (url.contains("https://"))
                        {
                            url = url.replace("https://", "");
                        }

                        String type = prefs.getString("whitelist_state_preference", "-1");
                        boolean accept = true;

                        if (!type.equals(Constants.NOLISTMODE))
                        {
                            String list = prefs.getString("list_elements_preference", "NA");
                            String[] listElements = list.split(",");

                            if (type.equals(Constants.BLACKLISTMODE))
                            {
                                for (int i = 0; i < listElements.length; i++)
                                {
                                    if (url.contains(listElements[i]))
                                    {
                                        accept = false;
                                    }
                                }
                            } else if (type.equals(Constants.WHITELISTMODE))
                            {
                                accept = false;
                                for (int i = 0; i < listElements.length; i++)
                                {
                                    if (url.contains(listElements[i]))
                                    {
                                        accept = true;
                                    }
                                }
                            }
                        }

                        byte[] transferResponse = pb.buildTransferResponsePacket(pubKey, accept, counter);
                        this.output.write(transferResponse); // SEND TRANSFER RESPONSE
                        this.output.flush();
                        counter++;

                        if (accept)
                        {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + url));
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        }
                        break;
                    }
                }
            } catch (SocketTimeoutException e)
            {
                Log.d("TIMEOUT", "server error");
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class ServerThread implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                Socket socket;

                ServerSocket server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(Constants.TCPPORT));

                while (!Thread.currentThread().isInterrupted())
                {
                    socket = server.accept();
                    if (Globals.register.checkRegister(socket.getInetAddress().getHostAddress()))
                    {
                        TransferServerThread tThread = new TransferServerThread(socket);
                        new Thread(tThread).start();
                    } else
                    {
                        socket.close();
                    }
                }
            } catch (IOException e)
            {
                stopSelf();
                e.printStackTrace();
            }
        }
    }

    public void startRegisterService()
    {
        registerThread = new Thread(new BroadCastListenerThread());
        registerThread.start();

        broadCastThread = new Thread(new BroadCastThread());
        broadCastThread.start();
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
                            byte[] messageArray;
                            messageArray = pb.buildResponsePacket(context);
                            sock.send(new DatagramPacket(messageArray, messageArray.length, InetAddress.getByName(ipAddress), Constants.BROADCASTPORT));
                        } catch (Exception e)
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
                Globals.register.removeRegister(ipAddress);
                break;
        }
    }

    private class BroadCastListenerThread implements Runnable
    {

        @Override
        public void run()
        {
            byte[] messageArray = new byte[Constants.BROADCASTSIZE];
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket(Constants.BROADCASTPORT, InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(messageArray, messageArray.length);
                while (!Thread.interrupted())
                {
                    socket.receive(packet);
                    String ipAddress = packet.getAddress().toString();

                    //if the packet is from you, ignore it
                    if (!ipAddress.contains(getIPAddress()))
                    {
                        BroadCastPacket temp = pb.parseBroadcastPacket(messageArray);

                        // register request
                        if (temp.getTYPE() == Constants.REGISTER)
                        {
                            register(temp, ipAddress);
                        }
                    }
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                if (socket != null)
                {
                    socket.disconnect();
                    socket.close();
                }
            }
        }
    }

    public String getIPAddress() throws IOException
    {
        WifiManager wifi = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifi.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    private class BroadCastThread implements Runnable
    {
        @Override
        public void run()
        {
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                byte[] messageArray;
                byte[] temp = pb.buildHelloPacket(context);
                int counter = 0;

                while (!Thread.interrupted())
                {
                    if (counter++ > 3)
                    {
                        counter = 0;
                        Globals.register = new Registry();
                    }
                    messageArray = temp;
                    socket.send(new DatagramPacket(messageArray, messageArray.length, getBroadcastAddress(), Constants.BROADCASTPORT));
                    Thread.sleep(5000);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                if (socket != null)
                {
                    socket.close();
                }
                stopSelf();
            }
        }
    }

    private InetAddress getBroadcastAddress() throws IOException
    {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
        {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }
}
