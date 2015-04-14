/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flashcast;

/**
 *
 * @author Jake
 */
public class WebQuery {
    private String _type;
    private String _value;
    private String _destination;
    
    public WebQuery(String queryDest, String queryType, String queryValue)
    {
        _type = queryType;
        _value = queryValue;
        _destination = queryDest;
    }
    
    public String getType()
    {
        return _type;
    }
    
    public String getValue()
    {
        return _value;
    }
    
    public String getDest()
    {
        return _destination;
    }
    
    public String toString()
    {
        return this.getDest() + " => " + this.getType() + ", " + this.getValue();
    }
}
