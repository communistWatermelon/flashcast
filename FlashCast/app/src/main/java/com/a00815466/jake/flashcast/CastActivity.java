package com.a00815466.jake.flashcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by Jake on 2015-02-07.
 */
public class CastActivity extends Activity
{
    private String ipAddress;
    private String publicKey;
    private PacketFunctions pb;
    private Button fileButton;
    private Button linkButton;
    private TextView edit;
    private Context context;
    private TextView textView;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        String shareAction = i.getAction();
        Bundle shareBundle = i.getExtras();

        setContentView(R.layout.cast_layout);
        edit = (TextView) findViewById(R.id.fileTextBox);
        fileButton = (Button) findViewById(R.id.sendFileButton);
        fileButton.setEnabled(false);
        linkButton = (Button) findViewById(R.id.sendLinkButton);
        textView = (TextView) findViewById(R.id.linkText);

        if (Intent.ACTION_SEND.equals(shareAction))
        {
            for (String key : shareBundle.keySet())
            {
                Object o = shareBundle.get(key);
                if (o.getClass().equals(String.class))
                {
                    String temp = (String) shareBundle.get(key);
                    if (temp.contains("http"))
                    {
                        int url = temp.indexOf("http");
                        if (url != 0)
                        {
                            temp = temp.substring(url);
                        }
                        textView.setText(temp);
                    }
                } else if (key.equals(Intent.EXTRA_STREAM))
                {
                    Uri uri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
                    String path = uri.getPath();
                    File file = new File(path);
                    if (file.exists())
                    {
                        edit.setText(uri.getPath());
                        fileButton.setEnabled(true);
                    }
                }
            }
        }

        context = getApplicationContext();
        edit.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    linkButton.performClick();
                    sendLink(v);
                    return true;
                }
                return false;
            }
        });

        pb = new PacketFunctions();
        Intent intent = getIntent();
        if (intent.hasExtra("hashmap"))
        {
            HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra("hashmap");
            ipAddress = hashMap.get("ip");
            publicKey = hashMap.get("public");
        }
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
            _isFile = isFile; // this is just to make the unused variable warning go away
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
                byte[] req2;
                socket = new Socket(_ip, Constants.TCPPORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                socket.setSoTimeout(Constants.TIMEOUT);

                byte buffer[] = new byte[Constants.CIPHERSIZE];
                Arrays.fill(buffer, (byte) 0);

                if (_isFile)
                {
                    req2 = pb.buildFileSendRequestPacket(_pubKey, _sendFile.getName(), _sendFile.length());
                }
                else
                {
                    req2 = pb.buildSendRequestPacket(_pubKey, _link.length());
                }
                out.write(req2);
                counter++;

                while (in.read(buffer, 0, buffer.length) > -1)
                {
                    Packet response = pb.parsePacket(buffer, context);
                    if (response.getSEQ() == counter && response.getTYPE() == Constants.DIRECTION) // Checking send response
                    {
                        if (response.getCODE() == Constants.ACCEPTREQ)
                        {
                            counter++;
                            Log.d("REQUEST", "ACCEPTED");
                            break;
                        } else
                        {
                            counter++;
                            Log.d("REQUEST", "DENIED");
                            return;
                        }
                    }
                }

                try
                {
                    if (_isFile) // TRANSFER STUFF
                    {
                        FileInputStream fileInputStream;
                        File file = new File(_sendFile.getPath());
                        byte[] bFile = new byte[(int) file.length()];
                        byte[] packetData;

                        //convert file into array of bytes
                        fileInputStream = new FileInputStream(file);
                        fileInputStream.read(bFile);
                        fileInputStream.close();

                        int multiples = bFile.length / (Constants.MAXSIZE-7);
                        int remainder = bFile.length % (Constants.MAXSIZE-7);

                        for (int i = 0; i < multiples; i++)
                        {
                            packetData = Arrays.copyOfRange(bFile, i * (Constants.MAXSIZE-7), (i + 1) * (Constants.MAXSIZE-7));
                            out.write(pb.buildTransferPacket(packetData, Constants.FILE, _pubKey, counter++));
                            Log.d("TRANSFER", "PACKET " + Integer.toString(counter));
                            out.flush();
                            Thread.sleep(10);
                        }

                        if (remainder > 0)
                        {
                            packetData = Arrays.copyOfRange(bFile, bFile.length - remainder, bFile.length);
                            out.write(pb.buildTransferPacket(packetData, Constants.FILE, _pubKey, counter++));
                            Log.d("TRANSFER", "PACKET " + Integer.toString(counter));
                            out.flush();
                        }
                    } else
                    {
                        if (!_link.isEmpty())
                        {
                            int size = _link.length();
                            byte[] packetData;
                            int multiples = size / (Constants.MAXSIZE - 7);
                            int remainder = size % (Constants.MAXSIZE - 7);

                            for (int i = 0; i < multiples; i++)
                            {
                                packetData = Arrays.copyOfRange(_link.getBytes(), i * (Constants.MAXSIZE - 7), (i + 1) * (Constants.MAXSIZE - 7));
                                out.write(pb.buildTransferPacket(packetData, Constants.LINK, _pubKey, counter++));
                                Log.d("TRANSFER", "PACKET " + Integer.toString(counter));
                                out.flush();
                            }

                            if (remainder > 0)
                            {
                                packetData = Arrays.copyOfRange(_link.getBytes(), _link.getBytes().length - remainder, _link.getBytes().length);
                                out.write(pb.buildTransferPacket(packetData, Constants.LINK, _pubKey, counter++));
                                Log.d("TRANSFER", "PACKET " + Integer.toString(counter));
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
                    Packet response = pb.parsePacket(buffer, context);
                    if (response.getSEQ() == counter%10 && response.getTYPE() == Constants.TRANSFER)
                    {
                        if (response.getCODE() == Constants.ACCEPTREQ)
                        {
                            counter++;
                            CastActivity.this.finish();
                            Log.d("TRANSFER", "ACCEPTED");
                            toastUser("Success!");
                            break;
                        } else
                        {
                            counter++;
                            Log.d("TRANSFER", "DENIED");
                            toastUser("Failure: Server Denied Request");
                            break;
                        }
                    }
                }

                out.close();
                in.close();

            } catch (SocketTimeoutException e)
            {
                toastUser("Timeout on transfer");
                Log.d("TIMEOUT","error");
            } catch (IOException e)
            {
                toastUser("Unspecified Error");
                e.printStackTrace();
            }
        }
    }

    public void toastUser(final String text)
    {

        CastActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(CastActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void chooseFile(View view)
    {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());
//        intent.putExtra(FileDialog.START_PATH, "/sdcard");
        intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
        startActivityForResult(intent, 1);
    }

    public void sendLink(View view)
    {
        String link = textView.getText().toString();
        new Thread(new TransferClientThread(ipAddress, publicKey, link)).start();
    }

    public void sendFile(View view)
    {
        String link = edit.getText().toString();
        new Thread(new TransferClientThread(ipAddress, publicKey, link, true)).start();
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == 1)
            {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                edit.setText(filePath);
                fileButton.setEnabled(true);
            }

        } else if (resultCode == Activity.RESULT_CANCELED)
        {
            edit.setText("");
            fileButton.setEnabled(false);
        }
    }
}
