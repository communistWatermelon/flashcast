package flashcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by Jake on 2015-02-07.
 */
public class CastActivity
{
    private String ipAddress;
    private String publicKey;
    private PacketFunctions pb;
    private String fileLocation;

    public CastActivity(WebQuery query, Registry r)
    {
        pb = new PacketFunctions();
        ipAddress = query.getDest();
        publicKey = r.getKeyFromIP(ipAddress, true);
    }

    private class TransferClientThread implements Runnable
    {
        private Socket socket;
        private InetAddress _ip;
        private String _link;
        private String _pubKey;
        private File _sendFile;
        private boolean _isFile;
        private int counter = 0;


        public TransferClientThread(String ip, String pubKey, String link)
        {
            try
            {
                _ip = InetAddress.getByName(ip);
            } catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
            _link = link;
            _pubKey = pubKey;
        }

        public TransferClientThread(String ip, String pubKey, String file, boolean isFile)
        {
            _isFile = isFile || true; // this is just to make the unused variable warning go away
            try
            {
                _ip = InetAddress.getByName(ip);
            } catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
            _sendFile = new File(file);
            _pubKey = pubKey;
            _link = "";
        }

        @Override
        public void run()
        {
            try
            {
                byte[] req2 = new byte[Constants.DATASIZE];
                socket = new Socket(_ip, Constants.TCPPORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                byte buffer[] = new byte[Constants.CIPHERSIZE];
                Arrays.fill(buffer, (byte) 0);

                if (_isFile)
                {
                    // get file size in bytes
                    req2 = pb.buildFileSendRequestPacket(_pubKey, _sendFile.getName(), _sendFile.length());
                }
                else
                {
                    req2 = pb.buildSendRequestPacket(_pubKey, _link.length());
                }
                out.write(req2); // SEND REQUEST
                counter++;

                while (in.read(buffer, 0, buffer.length) > -1)
                {
                    Packet response = pb.parsePacket(buffer);
                    if (response.getSEQ() == counter && response.getTYPE() == Constants.DIRECTION) // Checking send response
                    {
                        if (response.getCODE() == Constants.ACCEPTREQ)
                        {
                            counter++;
                            System.out.println("Request ACCEPTED");
                            break;
                        } else
                        {
                            counter++;
                            System.out.println("Request DENIED");
                            return;
                        }
                    }
                }

                try
                {
                    if (_isFile) // TRANSFER STUFF
                    {
                        FileInputStream fileInputStream = null;
                        File file = new File(_sendFile.getPath());
                        byte[] bFile = new byte[(int) file.length()];
                        byte[] packetData;

                        fileInputStream = new FileInputStream(file);
                        fileInputStream.read(bFile);
                        fileInputStream.close();

                        int multiples = bFile.length / (Constants.MAXSIZE-7);
                        int remainder = bFile.length % (Constants.MAXSIZE-7);

                        for (int i = 0; i < multiples; i++)
                        {
                            packetData = Arrays.copyOfRange(bFile, i * (Constants.MAXSIZE-7), (i + 1) * (Constants.MAXSIZE-7));
                            out.write(pb.buildTransferPacket(packetData, Constants.FILE, _pubKey, counter++));
                            System.out.println("Transfer packet " + Integer.toString(counter));
                            out.flush();
                        }

                        if (remainder > 0)
                        {
                            packetData = Arrays.copyOfRange(bFile, bFile.length - remainder, bFile.length);
                            out.write(pb.buildTransferPacket(packetData, Constants.FILE, _pubKey, counter++));
                            System.out.println("Transfer packet " + Integer.toString(counter));
                            out.flush();
                        }

                    } else
                    {
                        if (!_link.isEmpty())
                        {
                            int size = _link.length();
                            if (size > Constants.MAXSIZE-7)
                            {
                                byte[] packetData;
                                int multiples = size / (Constants.MAXSIZE - 7);
                                int remainder = size % (Constants.MAXSIZE - 7);

                                for (int i = 0; i < multiples; i++)
                                {
                                    packetData = Arrays.copyOfRange(_link.getBytes(), i * (Constants.MAXSIZE-7), (i + 1) * (Constants.MAXSIZE-7));
                                    out.write(pb.buildTransferPacket(packetData, Constants.LINK, _pubKey, counter++));
                                    System.out.println("TRANSFER PACKET " + Integer.toString(counter));
                                    out.flush();
                                }

                                if (remainder > 0)
                                {
                                    packetData = Arrays.copyOfRange(_link.getBytes(), _link.getBytes().length - remainder, _link.getBytes().length);
                                    out.write(pb.buildTransferPacket(packetData, Constants.LINK, _pubKey, counter++));
                                    System.out.println("TRANSFER PACKET " + Integer.toString(counter));
                                    out.flush();
                                }

                            } else
                            {
                                out.write(pb.buildTransferPacket(_link.getBytes(), Constants.LINK, _pubKey, counter++));  // SENDING TRANSFER
                                out.flush();
                            }
                        }
                    }

                    buffer = new byte[Constants.CIPHERSIZE];
                    Arrays.fill(buffer, (byte) 0);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                out.write(pb.buildSendFinishedPacket(_pubKey, counter++));  // SENDING FINISHED
                out.flush();

                while (in.read(buffer, 0, buffer.length) > -1) // AWAIT TRANSFER RESPONSE
                {
                    Packet response = pb.parsePacket(buffer);
                    if (response.getSEQ() == counter%10 && response.getTYPE() == Constants.TRANSFER)
                    {
                        if (response.getCODE() == Constants.ACCEPTREQ)
                        {
                            counter++;
                            System.out.println("Transfer ACCEPTED");
                            break;
                        } else
                        {
                            counter++;
                            System.out.println("Transfer DENIED");
                            // TODO toast the user with an error
                            break;
                        }
                    }
                }


                out.close();
                in.close();

            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendLink(String link)
    {
        new Thread(new TransferClientThread(ipAddress, publicKey, link)).start();
    }

    public void sendFile(String file)
    {
        new Thread(new TransferClientThread(ipAddress, publicKey, file, true)).start();
    }
    
    public void sendCast(WebQuery query)
    {
        if(query.getType().equals("link"))
        {
            sendLink(query.getValue());
        } else if (query.getType().equals("file")) 
        {
            sendFile(query.getValue());
        }
    }
}
