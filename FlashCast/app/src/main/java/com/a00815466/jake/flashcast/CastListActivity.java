package com.a00815466.jake.flashcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jake on 2015-02-02.
 */
public class CastListActivity extends Activity
{
    public static Context context;
    private ListView listView;
    private SwipeRefreshLayout swipeLayout;
    private String from[] = new String[]{"ip", "public"};
    private int to[] = new int[]{R.id.listItemView1, R.id.listItemView2};
    private Boolean share;
    private String shareAction;
    private Bundle shareBundle;

    private Handler myHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what == 0)
            {
                SimpleAdapter adapter = new SimpleAdapter(context, Globals.register.getRegistry(), R.layout.map_adapter_item, from, to);
                listView.setAdapter(adapter);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), UserSettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        share = false;
        Intent i = getIntent();
        shareAction = i.getAction();
        shareBundle = i.getExtras();

        if (Intent.ACTION_SEND.equals(shareAction))
        {
            share = true;
            setContentView(R.layout.cast_list_share_layout);
        }
        else
        {
            if (!Globals.serviceRunning)
            {
                Globals.serviceRunning = true;
                Intent FlashCastServiceIntent = new Intent(this, FlashCastService.class);
                startService(FlashCastServiceIntent);
            }
            setContentView(R.layout.cast_list_layout);
        }

        CastListActivity.context = getApplicationContext();
        listView = (ListView) findViewById(R.id.listView);

        myHandler.sendEmptyMessage(0);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myHandler.sendEmptyMessage(0);
                        swipeLayout.setRefreshing(false);
                    }
                }, 2000);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                HashMap<String, String> item = ((HashMap<String, String>) listView.getItemAtPosition(position));
                Intent intent = new Intent(context, CastActivity.class);
                intent.putExtra("hashmap", item);
                if (share)
                {
                    intent.setAction(shareAction);
                    intent.putExtras(shareBundle);
                }
                startActivity(intent);
            }
        });

        Thread refresh = new Thread(new refreshThread());
        refresh.start();
    }

    private class refreshThread implements Runnable
    {
        @Override
        public void run()
        {
            Timer myTimer = new Timer();
            myTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    myHandler.sendEmptyMessage(0);
                }
            }, 0, 2000);
        }
    }
}
