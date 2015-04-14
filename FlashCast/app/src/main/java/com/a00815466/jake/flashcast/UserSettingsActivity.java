package com.a00815466.jake.flashcast;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Jake on 2015-03-16.
 */
public class UserSettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
