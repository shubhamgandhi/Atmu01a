package atmu.atmu01a;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    int rProgress = 0;
    int gProgress = 0;
    int bProgress = 0;

    int clickedPixel;

    // arrays that hold rgb data of strips
    final char[][] s1 = new char[11][3];
    final char[][] s2 = new char[11][3];
    final char[][] s3 = new char[11][3];

    BluetoothAdapter myBluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    ListView myListView;
    ArrayAdapter<String> BTArrayAdapter;

    BluetoothSocket socket;
    OutputStream btOutputStream;

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "00:00:00:00:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // zero out array
        for(int i = 0; i < 11; i++) {
            for(int j = 0; j < 3; j++) {
                s1[i][j] = 0; s2[i][j] = 0; s3[i][j] = 0;
            }
        }

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        TextView statusText = (TextView) findViewById(R.id.textDeviceID);
        if (myBluetoothAdapter == null) {
            statusText.setText("Bluetooth not supported on this phone");
        }
        else {
            statusText.setText("Waiting to connect");
        }

        //declare button click listeners
        Button pre1 = (Button) findViewById(R.id.colorPicker);
        Button pre2 = (Button) findViewById(R.id.btnPreset2);
        Button pre3 = (Button) findViewById(R.id.btnPreset3);
        Button send = (Button) findViewById(R.id.btnSend);

        pre1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                colorPicker();
            }
        });

        pre2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset2();
            }
        });

        pre3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset3();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendToAtmu();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);

        if(item.getItemId()==R.id.connect) {
            if (!myBluetoothAdapter.isEnabled()) {
                Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

                while(!myBluetoothAdapter.isEnabled()) {
                    //delay while adapter is turning on
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            showBTDialog();
        }

        else if(item.getItemId()==R.id.action_settings) {
            // TODO
        }

        return true;
    }

    public void showBTDialog() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.bt_list, (ViewGroup) findViewById(R.id.bt_list));

        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(Viewlayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        myListView = (ListView) Viewlayout.findViewById(R.id.BTList);
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);
        myListView.setClickable(true);

        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
                // TODO Auto-generated method stub

                myBluetoothAdapter.cancelDiscovery();
                final String info = ((TextView) arg1).getText().toString();

                //get the device address when click the device item
                String address = info.substring(info.length()-17);

                //connect to the device when item is clicked
                BluetoothDevice connect_device = myBluetoothAdapter.getRemoteDevice(address);

                try {
                    socket = connect_device.createRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect();
                    if(socket.isConnected()) {
                        Toast.makeText(getApplicationContext(),"Connection successful",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Connection unsuccessful", Toast.LENGTH_SHORT).show();
                    }

                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });
        // put it's one to the adapter
        for(BluetoothDevice device : pairedDevices)
            BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());

                    // Button OK
                    popDialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }

                });

        // Create popup and show
        popDialog.create();
        popDialog.show();

    }

    public void sendToAtmu() {

        if(socket!=null) {
            try {
                btOutputStream = socket.getOutputStream();
                char test = 1;

                for(int i = 0; i < 11; i++) {
                    btOutputStream.write(s1[i][0]);
                    btOutputStream.write(s1[i][1]);
                    btOutputStream.write(s1[i][2]);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
        else {
            Toast.makeText(getApplicationContext(), "No bluetooth connection", Toast.LENGTH_SHORT).show();
        }

    }

    public void pixelClick(View view) {
        clickedPixel = view.getId();
        rProgress = 0; gProgress = 0; bProgress = 0;

        showPixelDialog();
    }

    public void showPixelDialog() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.popup, (ViewGroup) findViewById(R.id.popup));

        final TextView popupRText = (TextView) Viewlayout.findViewById(R.id.popupRText);
        final TextView popupGText = (TextView) Viewlayout.findViewById(R.id.popupGText);
        final TextView popupBText = (TextView) Viewlayout.findViewById(R.id.popupBText);

        final ImageView popupPixel = (ImageView) Viewlayout.findViewById(R.id.popupPixel);
        final ImageView setPixel = (ImageView) findViewById(clickedPixel);


        popDialog.setTitle("Select RGB values ");
        popDialog.setView(Viewlayout);

        //  seekBarR
        final SeekBar seekR = (SeekBar) Viewlayout.findViewById(R.id.seekBarR);
        seekR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Set progress text
                popupRText.setText("R: " + progress);
                rProgress = progress;

                //Set background color for popup pixel and strip pixel
                popupPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
                setPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        //  seekBarG
        final SeekBar seekG = (SeekBar) Viewlayout.findViewById(R.id.seekBarG);
        seekG.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                popupGText.setText("G: " + progress);
                gProgress = progress;

                //Set background color for popup pixel and strip pixel
                popupPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
                setPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        //  seekBarB
        final SeekBar seekB = (SeekBar) Viewlayout.findViewById(R.id.seekBarB);
        seekB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                popupBText.setText("B: " + progress);
                bProgress = progress;

                //Set background color for popup pixel and strip pixel
                popupPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
                setPixel.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });


        // Button OK
        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                });

        // Create popup and show
        popDialog.create();
        popDialog.show();


    }

    public void colorPicker() {

        final AlertDialog.Builder colorDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.color_picker_popup, (ViewGroup) findViewById(R.id.color_picker_popup));

        colorDialog.setTitle("Select a color");
        colorDialog.setView(Viewlayout);

        // Create popup and show
        colorDialog.create();
        colorDialog.show();

        // declare color picker buttons
        ImageView whiteButton = (ImageView) Viewlayout.findViewById(R.id.whiteButton);
        ImageView redButton = (ImageView) Viewlayout.findViewById(R.id.redButton);
        ImageView greenButton = (ImageView) Viewlayout.findViewById(R.id.greenButton);
        ImageView blueButton = (ImageView) Viewlayout.findViewById(R.id.blueButton);
        ImageView orangeButton = (ImageView) Viewlayout.findViewById(R.id.orangeButton);
        ImageView yellowButton = (ImageView) Viewlayout.findViewById(R.id.yellowButton);
        ImageView cyanButton = (ImageView) Viewlayout.findViewById(R.id.cyanButton);
        ImageView pinkButton = (ImageView) Viewlayout.findViewById(R.id.pinkButton);
        ImageView purpleButton = (ImageView) Viewlayout.findViewById(R.id.purpleButton);
        ImageView maroonButon = (ImageView) Viewlayout.findViewById(R.id.maroonButton);
        ImageView lavenderButton = (ImageView) Viewlayout.findViewById(R.id.lavenderButton);
        ImageView blackButton = (ImageView) Viewlayout.findViewById(R.id.blackButton);

        // declare all the imageviews
        final ImageView s1pixel1 = (ImageView) findViewById(R.id.s1pixel1);
        final ImageView s1pixel2 = (ImageView) findViewById(R.id.s1pixel2);
        final ImageView s1pixel3 = (ImageView) findViewById(R.id.s1pixel3);
        final ImageView s1pixel4 = (ImageView) findViewById(R.id.s1pixel4);
        final ImageView s1pixel5 = (ImageView) findViewById(R.id.s1pixel5);
        final ImageView s1pixel6 = (ImageView) findViewById(R.id.s1pixel6);
        final ImageView s1pixel7 = (ImageView) findViewById(R.id.s1pixel7);
        final ImageView s1pixel8 = (ImageView) findViewById(R.id.s1pixel8);
        final ImageView s1pixel9 = (ImageView) findViewById(R.id.s1pixel9);
        final ImageView s1pixel10 = (ImageView) findViewById(R.id.s1pixel10);
        final ImageView s1pixel11 = (ImageView) findViewById(R.id.s1pixel11);

        final ImageView s2pixel1 = (ImageView) findViewById(R.id.s2pixel1);
        final ImageView s2pixel2 = (ImageView) findViewById(R.id.s2pixel2);
        final ImageView s2pixel3 = (ImageView) findViewById(R.id.s2pixel3);
        final ImageView s2pixel4 = (ImageView) findViewById(R.id.s2pixel4);
        final ImageView s2pixel5 = (ImageView) findViewById(R.id.s2pixel5);
        final ImageView s2pixel6 = (ImageView) findViewById(R.id.s2pixel6);
        final ImageView s2pixel7 = (ImageView) findViewById(R.id.s2pixel7);
        final ImageView s2pixel8 = (ImageView) findViewById(R.id.s2pixel8);
        final ImageView s2pixel9 = (ImageView) findViewById(R.id.s2pixel9);
        final ImageView s2pixel10 = (ImageView) findViewById(R.id.s2pixel10);
        final ImageView s2pixel11 = (ImageView) findViewById(R.id.s2pixel11);

        final ImageView s3pixel1 = (ImageView) findViewById(R.id.s3pixel1);
        final ImageView s3pixel2 = (ImageView) findViewById(R.id.s3pixel2);
        final ImageView s3pixel3 = (ImageView) findViewById(R.id.s3pixel3);
        final ImageView s3pixel4 = (ImageView) findViewById(R.id.s3pixel4);
        final ImageView s3pixel5 = (ImageView) findViewById(R.id.s3pixel5);
        final ImageView s3pixel6 = (ImageView) findViewById(R.id.s3pixel6);
        final ImageView s3pixel7 = (ImageView) findViewById(R.id.s3pixel7);
        final ImageView s3pixel8 = (ImageView) findViewById(R.id.s3pixel8);
        final ImageView s3pixel9 = (ImageView) findViewById(R.id.s3pixel9);
        final ImageView s3pixel10 = (ImageView) findViewById(R.id.s3pixel10);
        final ImageView s3pixel11 = (ImageView) findViewById(R.id.s3pixel11);

        final ImageView strip1[] = {s1pixel1, s1pixel2, s1pixel3, s1pixel4,
                s1pixel5, s1pixel6, s1pixel7, s1pixel8, s1pixel9, s1pixel10, s1pixel11 };

        final ImageView strip2[] = {s2pixel1, s2pixel2, s2pixel3, s2pixel4,
                s2pixel5, s2pixel6, s2pixel7, s2pixel8, s2pixel9, s2pixel10, s2pixel11 };

        final ImageView strip3[] = {s3pixel1, s3pixel2, s3pixel3, s3pixel4,
                s3pixel5, s3pixel6, s3pixel7, s3pixel8, s3pixel9, s3pixel10, s3pixel11};



        whiteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(255, 255, 255));
                    s1[i][0] = (char) (255); s1[i][1] = (char) (255); s1[i][2] = (char) (255);
                    strip2[i].setBackgroundColor(Color.rgb(255, 255, 255));
                    s2[i][0] = (char) (255); s2[i][1] = (char) (255); s2[i][2] = (char) (255);
                    strip3[i].setBackgroundColor(Color.rgb(255, 255, 255));
                    s3[i][0] = (char) (255); s3[i][1] = (char) (255); s3[i][2] = (char) (255);
                }
            }

        });

        redButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(255, 0, 0));
                    s1[i][0] = (char) (255); s1[i][1] = (char) (0); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(255, 0, 0));
                    s2[i][0] = (char) (255); s2[i][1] = (char) (0); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(255, 0, 0));
                    s3[i][0] = (char) (255); s3[i][1] = (char) (0); s3[i][2] = (char) (0);
                }
            }
        });

        greenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0, 255, 0));
                    s1[i][0] = (char) (0); s1[i][1] = (char) (255); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(0, 255, 0));
                    s2[i][0] = (char) (0); s2[i][1] = (char) (255); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(0, 255, 0));
                    s3[i][0] = (char) (0); s3[i][1] = (char) (255); s3[i][2] = (char) (0);
                }
            }
        });

        blueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0, 0, 255));
                    s1[i][0] = (char) (0); s1[i][1] = (char) (0); s1[i][2] = (char) (255);
                    strip2[i].setBackgroundColor(Color.rgb(0, 0, 255));
                    s2[i][0] = (char) (0); s2[i][1] = (char) (0); s2[i][2] = (char) (255);
                    strip3[i].setBackgroundColor(Color.rgb(0, 0, 255));
                    s3[i][0] = (char) (0); s3[i][1] = (char) (0); s3[i][2] = (char) (255);
                }
            }
        });

        orangeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(255, 0xa5, 0));
                    s1[i][0] = (char) (255); s1[i][1] = (char) (0xa5); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(255, 0xa5, 0));
                    s2[i][0] = (char) (255); s2[i][1] = (char) (0xa5); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(255, 0xa5, 0));
                    s3[i][0] = (char) (255); s3[i][1] = (char) (0xa5); s3[i][2] = (char) (0);
                }
            }
        });

        yellowButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(255, 255, 0));
                    s1[i][0] = (char) (255); s1[i][1] = (char) (255); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(255, 255, 0));
                    s2[i][0] = (char) (255); s2[i][1] = (char) (255); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(255, 255, 0));
                    s3[i][0] = (char) (255); s3[i][1] = (char) (255); s3[i][2] = (char) (0);
                }
            }
        });

        cyanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0, 255, 255));
                    s1[i][0] = (char) (0); s1[i][1] = (char) (255); s1[i][2] = (char) (255);
                    strip2[i].setBackgroundColor(Color.rgb(0, 255, 255));
                    s2[i][0] = (char) (0); s2[i][1] = (char) (255); s2[i][2] = (char) (255);
                    strip3[i].setBackgroundColor(Color.rgb(0, 255, 255));
                    s3[i][0] = (char) (0); s3[i][1] = (char) (255); s3[i][2] = (char) (255);
                }
            }
        });

        pinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0xff, 0xc0, 0xcb));
                    s1[i][0] = (char) (0xff); s1[i][1] = (char) (0xc0); s1[i][2] = (char) (0xcb);
                    strip2[i].setBackgroundColor(Color.rgb(0xff, 0xc0, 0xcb));
                    s2[i][0] = (char) (0xff); s2[i][1] = (char) (0xc0); s2[i][2] = (char) (0xcb);
                    strip3[i].setBackgroundColor(Color.rgb(0xff, 0xc0, 0xcb));
                    s3[i][0] = (char) (0xff); s3[i][1] = (char) (0xc0); s3[i][2] = (char) (0xcb);
                }
            }
        });

        purpleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0xa0, 0x20, 0xf0));
                    s1[i][0] = (char) (0xa0); s1[i][1] = (char) (0x20); s1[i][2] = (char) (0xf0);
                    strip2[i].setBackgroundColor(Color.rgb(0xa0, 0x20, 0xf0));
                    s2[i][0] = (char) (0xa0); s2[i][1] = (char) (0x20); s2[i][2] = (char) (0xf0);
                    strip3[i].setBackgroundColor(Color.rgb(0xa0, 0x20, 0xf0));
                    s3[i][0] = (char) (0xa0); s3[i][1] = (char) (0x20); s3[i][2] = (char) (0xf0);
                }
            }
        });

        maroonButon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0x80, 0, 0));
                    s1[i][0] = (char) (0x80); s1[i][1] = (char) (0); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(0x80, 0, 0));
                    s2[i][0] = (char) (0x80); s2[i][1] = (char) (0); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(0x80, 0, 0));
                    s3[i][0] = (char) (0x80); s3[i][1] = (char) (0); s3[i][2] = (char) (0);
                }
            }
        });

        lavenderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0xe6, 0xe6, 0xfa));
                    s1[i][0] = (char) (0xe6); s1[i][1] = (char) (0xe6); s1[i][2] = (char) (0xfa);
                    strip2[i].setBackgroundColor(Color.rgb(0xe6, 0xe6, 0xfa));
                    s2[i][0] = (char) (0xe6); s2[i][1] = (char) (0xe6); s2[i][2] = (char) (0xfa);
                    strip3[i].setBackgroundColor(Color.rgb(0xe6, 0xe6, 0xfa));
                    s3[i][0] = (char) (0xe6); s3[i][1] = (char) (0xe6); s3[i][2] = (char) (0xfa);
                }
            }
        });

        blackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < 11; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(0, 0, 0));
                    s1[i][0] = (char) (0); s1[i][1] = (char) (0); s1[i][2] = (char) (0);
                    strip2[i].setBackgroundColor(Color.rgb(0, 0, 0));
                    s2[i][0] = (char) (0); s2[i][1] = (char) (0); s2[i][2] = (char) (0);
                    strip3[i].setBackgroundColor(Color.rgb(0, 0, 0));
                    s3[i][0] = (char) (0); s3[i][1] = (char) (0); s3[i][2] = (char) (0);
                }
            }
        });
    }

    public void setPreset2() {

        // declare all the imageviews
        final ImageView s1pixel1 = (ImageView) findViewById(R.id.s1pixel1);
        final ImageView s1pixel2 = (ImageView) findViewById(R.id.s1pixel2);
        final ImageView s1pixel3 = (ImageView) findViewById(R.id.s1pixel3);
        final ImageView s1pixel4 = (ImageView) findViewById(R.id.s1pixel4);
        final ImageView s1pixel5 = (ImageView) findViewById(R.id.s1pixel5);
        final ImageView s1pixel6 = (ImageView) findViewById(R.id.s1pixel6);
        final ImageView s1pixel7 = (ImageView) findViewById(R.id.s1pixel7);
        final ImageView s1pixel8 = (ImageView) findViewById(R.id.s1pixel8);
        final ImageView s1pixel9 = (ImageView) findViewById(R.id.s1pixel9);
        final ImageView s1pixel10 = (ImageView) findViewById(R.id.s1pixel10);
        final ImageView s1pixel11 = (ImageView) findViewById(R.id.s1pixel11);

        final ImageView s2pixel1 = (ImageView) findViewById(R.id.s2pixel1);
        final ImageView s2pixel2 = (ImageView) findViewById(R.id.s2pixel2);
        final ImageView s2pixel3 = (ImageView) findViewById(R.id.s2pixel3);
        final ImageView s2pixel4 = (ImageView) findViewById(R.id.s2pixel4);
        final ImageView s2pixel5 = (ImageView) findViewById(R.id.s2pixel5);
        final ImageView s2pixel6 = (ImageView) findViewById(R.id.s2pixel6);
        final ImageView s2pixel7 = (ImageView) findViewById(R.id.s2pixel7);
        final ImageView s2pixel8 = (ImageView) findViewById(R.id.s2pixel8);
        final ImageView s2pixel9 = (ImageView) findViewById(R.id.s2pixel9);
        final ImageView s2pixel10 = (ImageView) findViewById(R.id.s2pixel10);
        final ImageView s2pixel11 = (ImageView) findViewById(R.id.s2pixel11);

        final ImageView s3pixel1 = (ImageView) findViewById(R.id.s3pixel1);
        final ImageView s3pixel2 = (ImageView) findViewById(R.id.s3pixel2);
        final ImageView s3pixel3 = (ImageView) findViewById(R.id.s3pixel3);
        final ImageView s3pixel4 = (ImageView) findViewById(R.id.s3pixel4);
        final ImageView s3pixel5 = (ImageView) findViewById(R.id.s3pixel5);
        final ImageView s3pixel6 = (ImageView) findViewById(R.id.s3pixel6);
        final ImageView s3pixel7 = (ImageView) findViewById(R.id.s3pixel7);
        final ImageView s3pixel8 = (ImageView) findViewById(R.id.s3pixel8);
        final ImageView s3pixel9 = (ImageView) findViewById(R.id.s3pixel9);
        final ImageView s3pixel10 = (ImageView) findViewById(R.id.s3pixel10);
        final ImageView s3pixel11 = (ImageView) findViewById(R.id.s3pixel11);

        final ImageView strip1[] = {s1pixel1, s1pixel2, s1pixel3, s1pixel4,
                s1pixel5, s1pixel6, s1pixel7, s1pixel8, s1pixel9, s1pixel10, s1pixel11 };

        final ImageView strip2[] = {s2pixel1, s2pixel2, s2pixel3, s2pixel4,
                s2pixel5, s2pixel6, s2pixel7, s2pixel8, s2pixel9, s2pixel10, s2pixel11 };

        final ImageView strip3[] = {s3pixel1, s3pixel2, s3pixel3, s3pixel4,
                s3pixel5, s3pixel6, s3pixel7, s3pixel8, s3pixel9, s3pixel10, s3pixel11};

        for(int i = 0; i < 6; i++) {
            strip1[i].setBackgroundColor(Color.rgb(0, 255, 0));
                s1[i][0] = (char) (0);
                s1[i][1] = (char) (255);
                s1[i][2] = (char) (0);
            strip2[i].setBackgroundColor(Color.rgb(0, 255, 0));
                s2[i][0] = (char) (0);
                s2[i][1] = (char) (255);
                s2[i][2] = (char) (0);
            strip3[i].setBackgroundColor(Color.rgb(0, 255, 0));
                s3[i][0] = (char) (0);
                s3[i][1] = (char) (255);
                s3[i][2] = (char) (0);
        }
        for(int i = 6; i < 9; i++) {
            strip1[i].setBackgroundColor(Color.rgb(255, 255, 0));
                s1[i][0] = (char) (255);
                s1[i][1] = (char) (255);
                s1[i][2] = (char) (0);
            strip2[i].setBackgroundColor(Color.rgb(255, 255, 0));
                s2[i][0] = (char) (255);
                s2[i][1] = (char) (255);
                s2[i][2] = (char) (0);
            strip3[i].setBackgroundColor(Color.rgb(255, 255, 0));
                s3[i][0] = (char) (255);
                s3[i][1] = (char) (255);
                s3[i][2] = (char) (0);
        }
        for(int i = 9; i < 11; i++) {
            strip1[i].setBackgroundColor(Color.rgb(255, 0, 0));
                s1[i][0] = (char) (255);
                s1[i][1] = (char) (0);
                s1[i][2] = (char) (0);
            strip2[i].setBackgroundColor(Color.rgb(255, 0, 0));
                s2[i][0] = (char) (255);
                s2[i][1] = (char) (0);
                s2[i][2] = (char) (0);
            strip3[i].setBackgroundColor(Color.rgb(255, 0, 0));
                s3[i][0] = (char) (255);
                s3[i][1] = (char) (0);
                s3[i][2] = (char) (0);
        }

    }

    public void setPreset3() {
        // declare all the imageviews
        final ImageView s1pixel1 = (ImageView) findViewById(R.id.s1pixel1);
        final ImageView s1pixel2 = (ImageView) findViewById(R.id.s1pixel2);
        final ImageView s1pixel3 = (ImageView) findViewById(R.id.s1pixel3);
        final ImageView s1pixel4 = (ImageView) findViewById(R.id.s1pixel4);
        final ImageView s1pixel5 = (ImageView) findViewById(R.id.s1pixel5);
        final ImageView s1pixel6 = (ImageView) findViewById(R.id.s1pixel6);
        final ImageView s1pixel7 = (ImageView) findViewById(R.id.s1pixel7);
        final ImageView s1pixel8 = (ImageView) findViewById(R.id.s1pixel8);
        final ImageView s1pixel9 = (ImageView) findViewById(R.id.s1pixel9);
        final ImageView s1pixel10 = (ImageView) findViewById(R.id.s1pixel10);
        final ImageView s1pixel11 = (ImageView) findViewById(R.id.s1pixel11);

        final ImageView s2pixel1 = (ImageView) findViewById(R.id.s2pixel1);
        final ImageView s2pixel2 = (ImageView) findViewById(R.id.s2pixel2);
        final ImageView s2pixel3 = (ImageView) findViewById(R.id.s2pixel3);
        final ImageView s2pixel4 = (ImageView) findViewById(R.id.s2pixel4);
        final ImageView s2pixel5 = (ImageView) findViewById(R.id.s2pixel5);
        final ImageView s2pixel6 = (ImageView) findViewById(R.id.s2pixel6);
        final ImageView s2pixel7 = (ImageView) findViewById(R.id.s2pixel7);
        final ImageView s2pixel8 = (ImageView) findViewById(R.id.s2pixel8);
        final ImageView s2pixel9 = (ImageView) findViewById(R.id.s2pixel9);
        final ImageView s2pixel10 = (ImageView) findViewById(R.id.s2pixel10);
        final ImageView s2pixel11 = (ImageView) findViewById(R.id.s2pixel11);

        final ImageView s3pixel1 = (ImageView) findViewById(R.id.s3pixel1);
        final ImageView s3pixel2 = (ImageView) findViewById(R.id.s3pixel2);
        final ImageView s3pixel3 = (ImageView) findViewById(R.id.s3pixel3);
        final ImageView s3pixel4 = (ImageView) findViewById(R.id.s3pixel4);
        final ImageView s3pixel5 = (ImageView) findViewById(R.id.s3pixel5);
        final ImageView s3pixel6 = (ImageView) findViewById(R.id.s3pixel6);
        final ImageView s3pixel7 = (ImageView) findViewById(R.id.s3pixel7);
        final ImageView s3pixel8 = (ImageView) findViewById(R.id.s3pixel8);
        final ImageView s3pixel9 = (ImageView) findViewById(R.id.s3pixel9);
        final ImageView s3pixel10 = (ImageView) findViewById(R.id.s3pixel10);
        final ImageView s3pixel11 = (ImageView) findViewById(R.id.s3pixel11);

        final ImageView strip1[] = {s1pixel1, s1pixel2, s1pixel3, s1pixel4,
                s1pixel5, s1pixel6, s1pixel7, s1pixel8, s1pixel9, s1pixel10, s1pixel11 };

        final ImageView strip2[] = {s2pixel1, s2pixel2, s2pixel3, s2pixel4,
                s2pixel5, s2pixel6, s2pixel7, s2pixel8, s2pixel9, s2pixel10, s2pixel11 };

        final ImageView strip3[] = {s3pixel1, s3pixel2, s3pixel3, s3pixel4,
                s3pixel5, s3pixel6, s3pixel7, s3pixel8, s3pixel9, s3pixel10, s3pixel11 };

        for(int i = 0; i < 11; i++) {
            // Set background color of pixels and update array that holds RGB values
            strip1[i].setBackgroundColor(Color.rgb(23 * i, 23 * i, 255 - i));
                s1[i][0] = (char) (23*i);
                s1[i][1] = (char) (23*i);
                s1[i][2] = (char) (255-i);
            strip2[i].setBackgroundColor(Color.rgb(23 * i, 23 * i, 255 - i));
                s2[i][0] = (char) (23*i);
                s2[i][1] = (char) (23*i);
                s2[i][2] = (char) (255-i);
            strip3[i].setBackgroundColor(Color.rgb(23 * i, 23 * i, 255 - i));
                s3[i][0] = (char) (23*i);
                s3[i][1] = (char) (23*i);
                s3[i][2] = (char) (255-i);
        }
    }

}
