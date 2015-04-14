package flashcast;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity
{
    public static void main(String args[])
    {       
        KeyFunctions.generateKeys();
        CastListActivity castList = new CastListActivity();
        castList.start(Globals.register);
        
        try {
            Globals.u.start(Globals.register);
        } catch (Exception ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
