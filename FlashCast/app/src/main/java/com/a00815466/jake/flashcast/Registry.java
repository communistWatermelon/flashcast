package com.a00815466.jake.flashcast;

import java.util.ArrayList;
import java.util.HashMap;

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
                return registry.get(i).get("public");
            }
        }
        return "";
    }

    public void addRegister(String ip, String pubKey)
    {
        HashMap<String, String> temp = new HashMap<>();
        temp.put("ip", ip);
        temp.put("public", pubKey);
        if (!registry.contains(temp))
        {
            registry.add(temp);
        }
    }

    public void removeRegister(String ip)
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
}
