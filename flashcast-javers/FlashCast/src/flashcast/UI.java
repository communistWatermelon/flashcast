package flashcast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class UI 
{
    public void start(Registry r) throws Exception 
    {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler 
    {
        public void handle(HttpExchange t) throws IOException 
        {
            String query = t.getRequestURI().getQuery();
            
            if (t.getRequestURI().toString().contains("flashcast-api")) 
            {
                String body = Globals.register.toWebString();
                t.sendResponseHeaders(200, body.length());
                OutputStream os = t.getResponseBody();
                os.write(body.getBytes());
                os.close();
            } else if (t.getRequestURI().toString().contains("security")) {
                String header = getSettingsHTML();
                t.sendResponseHeaders(200, header.length());
                OutputStream os = t.getResponseBody();
                os.write(header.getBytes());
                os.close();
            } else if (t.getRequestURI().toString().contains("file-api")) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) 
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    String path = selectedFile.getAbsolutePath();
                    // send that data back to the user
                    t.sendResponseHeaders(200, path.length());
                    OutputStream os = t.getResponseBody();
                    os.write(path.getBytes());
                    os.close();
                } else {
                    return;
                }
            } else if (t.getRequestURI().toString().contains("send")) {                  
                try 
                {
                    WebQuery clientRequest = parseQuery(query);                    
                    CastActivity c = new CastActivity(clientRequest, Globals.register);
                    c.sendCast(clientRequest);
                    
                    byte[] buffer = new byte[1024]; 
                    t.getRequestBody().read(buffer);
                    t.sendResponseHeaders(200, 0);
                    OutputStream response = t.getResponseBody();
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (t.getRequestURI().toString().contains("settings-api")) {
                try
                {
                    Globals.settings = parseSettingsQuery(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (t.getRequestURI().toString().contains("current-settings")) { 
                String body = Globals.settings.toWebString();
                t.sendResponseHeaders(200, body.length());
                OutputStream os = t.getResponseBody();
                os.write(body.getBytes());
                os.close();
            } else {
                if (t.getRequestMethod().contains("GET")) 
                {            
                    String header = getHTML();
                    t.sendResponseHeaders(200, header.length());
                    OutputStream os = t.getResponseBody();
                    os.write(header.getBytes());
                    os.close();
                }
            }            
        }
    }
    
    public static String getHTML()
    {
        String temp = "";
        try {
//            URL tempPath = UI.class.getResource("/resources/temp.html");
            String t3 = UI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            t3 = t3.split("FlashCast.jar")[0];
            if (System.getProperty("os.name").contains("Windows"))
            {
                t3 = t3.substring(1);
            }
            
            temp = new String(Files.readAllBytes(Paths.get(t3 + "temp.html")));
        } catch (IOException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return temp;
        
    }
    
    public static String getSettingsHTML()
    {
        String temp = "";
        try {
            String t3 = UI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            t3 = t3.split("FlashCast.jar")[0];
            
            if (System.getProperty("os.name").contains("Windows"))
            {
                t3 = t3.substring(1);
            }
            
            temp = new String(Files.readAllBytes(Paths.get(t3 + "security.html")));
        } catch (IOException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return temp;
    }
    
    private static Settings parseSettingsQuery(String query) throws Exception
    {
        String[] tokens = query.split("&", 6);
        
        if (tokens.length == 6)
        {
            for (int i = 0; i < tokens.length; i++)
            {
                tokens[i] = tokens[i].split("=")[1];
            }
            
            if (tokens[5] == "0")
            {
                tokens[5] = "";
            }
            
            return new Settings(Boolean.parseBoolean(tokens[0]), Boolean.parseBoolean(tokens[1]), Boolean.parseBoolean(tokens[2]), Boolean.parseBoolean(tokens[3]), Integer.parseInt(tokens[4]), tokens[5]);
        } else 
        {
            throw new Exception();
        }
        
    }
    
    private static WebQuery parseQuery(String query) throws Exception
    {
        String[] tokens = query.split("&", 3);
        
        if (tokens.length == 3)
        {    
            Arrays.sort(tokens);

            for (int i = 0; i < 3; i++)
                if (i != 2)
                {
                    tokens[i] = tokens[i].split("=")[1];
                } else 
                {
                    tokens[i] = tokens[i].split("=", 2)[1];
                }

            return new WebQuery(tokens[0], tokens[1], tokens[2]);
        } else {
            throw new Exception();
        }
    }
}