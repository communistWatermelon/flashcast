package com.a00815466.jake.flashcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Jake on 2015-03-16.
 */
public class ShareActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        if (KeyFunctions.getPublicKey(context) == null || KeyFunctions.getPrivateKey(context) == null)
        {
            KeyFunctions.generateKeys(context);
        }

        Intent i = getIntent();
        Intent intent = new Intent(this, CastListActivity.class);
        intent.setAction(i.getAction());
        intent.putExtras(i.getExtras());
        startActivity(intent);
        finish();
    }
}
