package flashcast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jake on 2015-02-07.
 */
public class Registry
{
    private ArrayList<HashMap<String, String>> registry;

    public Registry()
    {
        registry = new ArrayList<>();
    }

    public ArrayList<HashMap<String, String>> getRegistry()
    {
        return registry;
    }

    public String getKeyFromIP(String ip)
    {
        ip = ip.substring(1).split(":")[0];
        for (int i = 0; i < registry.size(); i++)
        {
            if (registry.get(i).containsValue(ip))
            {
                String ttttt = registry.get(i).get("public");
                return ttttt;
            }
        }
        return "";
    }
    
    public String getKeyFromIP(String ip, boolean temp)
    {
        for (int i = 0; i < registry.size(); i++)
        {
            if (registry.get(i).containsValue(ip))
            {
                String ttttt = registry.get(i).get("public");
                return ttttt;
            }
        }
        return "";
    }

    public void addRegister(String ip, String pubKey)
    {
        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("ip", ip);
        temp.put("public", pubKey);
        if (!registry.contains(temp))
        {
            System.out.println("Registering " + ip);
            registry.add(temp);
        }
    }

    public void removeRegister(String ip, String pubKey)
    {
        for (int i = 0; i < registry.size(); i++)
        {
            if (registry.get(i).containsValue(ip))
            {
                registry.remove(i);
            }
        }
    }

    public boolean checkRegister(String ip)
    {
        for (int i = 0; i < registry.size(); i++)
        {
            if (registry.get(i).containsValue(ip))
            {
                return true;
            }
        }

        return false;
    }
    
    public int getSize()
    {
        return registry.size();
    }
            
    public String toWebString()
    {
        
        String html = "";
        try {
            String t3 = UI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            t3 = t3.split("FlashCast.jar")[0];
            
            if (System.getProperty("os.name").contains("Windows"))
            {
                t3 = t3.substring(1);
            }
            
            html = new String(Files.readAllBytes(Paths.get(t3 + "panel.html")));
        } catch (IOException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(Registry.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String temp = "";
        for (int i = 0; i < registry.size(); i++)
        {
            temp += "<div class=\"flip\" data-ip=\"" + registry.get(i).get("ip") + "\">";
            
            temp += html;
            
            temp += "</div>";
        }
        return temp;
    }
}
