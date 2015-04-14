package com.a00815466.jake.flashcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        KeyFunctions.generateKeys(context);
        Intent intent = new Intent(this, CastListActivity.class);
        startActivity(intent);
    }

}
