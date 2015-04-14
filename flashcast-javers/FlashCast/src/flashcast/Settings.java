/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flashcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


public class Settings 
{
    private String _password;
    private boolean _firewallEnabled;
    private boolean _allowLinks;
    private boolean _allowFiles;
    private boolean _allowRaw;
    private int _listOption;
    private ArrayList<String> _listElements;
    
    public Settings(boolean firewall, boolean link, boolean file, boolean raw, int filterType, String filterList)
    {
        this();
        _firewallEnabled = firewall;
        _allowLinks = link;
        _allowFiles = file;
        _allowRaw = raw;
        _listOption = filterType;
        
        filterList = filterList.replaceAll("\\+", "");
        
        if (!filterList.contains("0"))
        {
            _listElements = new ArrayList<String>(Arrays.asList(filterList.split(",")));
        } else 
        {
            _listElements = new ArrayList<String>(Arrays.asList(""));
        }
        
        saveSettings();
    }
    
    public Settings(String password)
    {
        _password = password;
        _firewallEnabled = true;
        _allowLinks = true;
        _allowFiles = false;
        _allowRaw = false;
        _listOption = Constants.WHITELIST;
        _listElements = new ArrayList<String>();
    }
    
    public Settings()
    {
        this(Constants.KEY);
        
        File config = new File("config.properties");
        if (config.exists())
        {
            readSettings();
        } else 
        {
            addToList("google");
            saveSettings();
        }
    }
    
    private void readSettings()
    {
        Properties prop = new Properties();
	InputStream input = null;
        
        try 
        {
            input = new FileInputStream("config.properties");
            prop.load(input);
            this._password = prop.getProperty("pass");
            this._firewallEnabled = Boolean.parseBoolean(prop.getProperty("FirewallEnabled"));
            this._allowLinks = Boolean.parseBoolean(prop.getProperty("AllowLinks"));
            this._allowFiles = Boolean.parseBoolean(prop.getProperty("AllowFiles"));
            this._allowRaw = Boolean.parseBoolean(prop.getProperty("AllowRaw"));
            this._listOption = Integer.parseInt(prop.getProperty("ListOption"));
            
            File file = new File("listed.properties");
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String str = new String(data, "UTF-8");
            _listElements = new ArrayList<String>(Arrays.asList(str.split(",")));    
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            if (input != null) 
            {
                try 
                {
                    input.close();
                } catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void saveSettings()
    {
        Properties prop = new Properties();
        OutputStream out = null;
        
        try 
        {
            out = new FileOutputStream("config.properties");
            prop.setProperty("pass", _password);
            prop.setProperty("FirewallEnabled", String.valueOf(_firewallEnabled));
            prop.setProperty("AllowLinks", String.valueOf(_allowLinks));
            prop.setProperty("AllowFiles", String.valueOf(_allowFiles));
            prop.setProperty("AllowRaw", String.valueOf(_allowRaw));
            prop.setProperty("ListOption", String.valueOf(_listOption));
            prop.store(out, null);
            
            out = new FileOutputStream("listed.properties");
            for (int i = 0; i < _listElements.size(); i++)
            {
                if (!_listElements.get(i).equals(""))
                {
                    out.write((_listElements.get(i) + ",").getBytes());
                } else 
                {
                    out.write("".getBytes());
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            if (out != null) 
            {
                try 
                {
                    out.close();
                } catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void addToList(String element)
    {
        _listElements.add(element);
        saveSettings();
    }
    
    public boolean toggleFirewall()
    {
        _firewallEnabled = !_firewallEnabled;
        saveSettings();
        return _firewallEnabled;
    }
    
    public boolean toggleLinks()
    {
        _allowLinks = !_allowLinks;
        saveSettings();
        return _allowLinks;
    }
    
    public boolean toggleFiles()
    {
        _allowFiles = !_allowFiles;
        saveSettings();
        return _allowFiles;
    }
    
    public boolean toggleRaw()
    {
        _allowRaw = !_allowRaw;
        saveSettings();
        return _allowRaw;
    }
    
    public int setListType(int i)
    {
        if (i != Constants.BLACKLIST || i != Constants.WHITELIST || i != Constants.NOLIST)
        {
            return -1;
        } else 
        {
            _listOption = i;
            saveSettings();
            return _listOption;
        }
    }
    
    public void removeFromList(String element)
    {
        if (_listElements.contains(element))
        {
            _listElements.remove(_listElements.indexOf(element));
            saveSettings();
        }
    }
    
    public boolean getLinkState()
    {
        readSettings();
        return _allowLinks;
    }
    
    public boolean getFileState()
    {
        readSettings();
        return _allowFiles;
    }
    
    public boolean validateURL(String url)
    {
        readSettings();
        if (_firewallEnabled)
        {
            if (_listOption == Constants.NOLIST)
            {
                return true;
            } 
            
            if (_listOption == Constants.BLACKLIST)
            {
                for (int i = 0; i < _listElements.size(); i++)
                {
                    if (url.contains(_listElements.get(i)) && !_listElements.get(i).isEmpty())
                    {
                        return false;
                    }
                }
                return true;
            } else 
            {
                for (int i = 0; i < _listElements.size(); i++)
                {
                    if (url.contains(_listElements.get(i)) && !_listElements.get(i).isEmpty())
                    {
                        return true;
                    }
                }
                return false;
            }
        } else 
        {
            return true;
        }
    }
    
    public String toWebString()
    {
        readSettings();
        String temp = "";
        temp += "firewall: " + _firewallEnabled + ", ";
        temp += "link: " + _allowLinks + ", ";
        temp += "file: " + _allowFiles + ", ";
        temp += "raw: " + _allowRaw + ", ";
        temp += "filterType: " + _listOption + ", ";
        if (_listElements.size() > 0)
        {
            temp += "filterList: ";
            for (int i = 0; i < _listElements.size(); i++)
            {
                temp += _listElements.get(i) + "-";
            }
        } else 
        {
            temp += "filterList: 0";
        }
        return temp;
    }
    
    public boolean getFirewallState()
    {
        return _firewallEnabled;
    }
}
