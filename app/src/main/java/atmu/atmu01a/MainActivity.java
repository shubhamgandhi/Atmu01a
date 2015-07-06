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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    static final int STRIP_SIZE = 11;
    static final int TUBE_QTY = 3;
    static final int LEDS_PER_PIXEL = 3;

    int pixelSelect;
    int tubeSelect;
    int rProgress = 0;
    int gProgress = 0;
    int bProgress = 0;

    // array that holds LED values to be pushed to Atmu
    final char[][] s1 = new char[STRIP_SIZE][LEDS_PER_PIXEL];
    final char[][] s2 = new char[STRIP_SIZE][LEDS_PER_PIXEL];
    final char[][] s3 = new char[STRIP_SIZE][LEDS_PER_PIXEL];
    final char[][][] strips = new char[TUBE_QTY][STRIP_SIZE][LEDS_PER_PIXEL];

    BluetoothAdapter myBluetoothAdapter;
    ArrayAdapter<String> BTArrayAdapter;
    BluetoothSocket socket;
    OutputStream btOutputStream;
    Set<BluetoothDevice> pairedDevices;
    ListView listViewBTDevices;

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "00:00:00:00:00:00";

    private PopupWindow presetWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // zero out array
        for(int i = 0; i < STRIP_SIZE; i++) {
            for(int j = 0; j < LEDS_PER_PIXEL; j++) {
                s1[i][j] = 0; s2[i][j] = 0; s3[i][j] = 0;
            }
        }

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        TextView statusText = (TextView) findViewById(R.id.textDeviceID);
        if (myBluetoothAdapter == null) {
            statusText.setText("Bluetooth not supported on this phone");
        }
        else {
            statusText.setText("Disconnected");
        }

        //create buttons, link buttons to XML views
        ImageButton btnPreset = (ImageButton) findViewById(R.id.btnSetPreset);
        ImageButton pre2 = (ImageButton) findViewById(R.id.btnSetLEDs);
        ImageButton btnSetTube = (ImageButton) findViewById(R.id.btnSetTube);
        ImageButton send = (ImageButton) findViewById(R.id.btnSend);

        //declare button click listeners
        btnPreset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPresetList();
            }
        });

        pre2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset2();
            }
        });

        btnSetTube.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showTubeList();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendToAtmu();
            }
        });

        Drawable myIcon = getDrawable(R.drawable.pixel_test);
        myIcon.mutate().setColorFilter(0xFF0000, PorterDuff.Mode.MULTIPLY);

    }

    //Options Menu
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
        final View viewLayout = inflater.inflate(R.layout.bt_list, (ViewGroup) findViewById(R.id.bt_list));

        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(viewLayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        listViewBTDevices = (ListView) viewLayout.findViewById(R.id.BTList);
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listViewBTDevices.setAdapter(BTArrayAdapter);
        listViewBTDevices.setClickable(true);

        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();

        listViewBTDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
                        Toast.makeText(getApplicationContext(),"Connection Successful",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Connection Unsuccessful", Toast.LENGTH_SHORT).show();
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

    //Send pixel array to Atmu
    public void sendToAtmu() {
        if(socket!=null) {
            // ghetto Attempt at making array of array of array
            try {
                btOutputStream = socket.getOutputStream();
                for (int cur_tube = 0; cur_tube < TUBE_QTY; cur_tube++) {
                    for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
                        for (int cur_LED = 0; cur_LED < LEDS_PER_PIXEL; cur_LED++) {
                            btOutputStream.write(strips[cur_tube][cur_pixel][cur_LED]);
                        }
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
//         if that mess above doesn't work uncomment this
//            try {
//                btOutputStream = socket.getOutputStream();
//
//                for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
//                    btOutputStream.write(s1[cur_pixel][0]);
//                    btOutputStream.write(s1[cur_pixel][1]);
//                    btOutputStream.write(s1[cur_pixel][2]);
//                    try {
//                        Thread.sleep(5);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
//                    btOutputStream.write(s2[cur_pixel][0]);
//                    btOutputStream.write(s2[cur_pixel][1]);
//                    btOutputStream.write(s2[cur_pixel][2]);
//                    try {
//                        Thread.sleep(5);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
//                    btOutputStream.write(s3[cur_pixel][0]);
//                    btOutputStream.write(s3[cur_pixel][1]);
//                    btOutputStream.write(s3[cur_pixel][2]);
//                    try {
//                        Thread.sleep(5);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No Bluetooth Connection", Toast.LENGTH_SHORT).show();
        }
    }

    //When clicking pixel
    public void pixelClick(View view) {
        pixelSelect = view.getId();
        rProgress = 0; gProgress = 0; bProgress = 0;

        showPixelDetail();
    }

    public void showPixelDetail() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View viewLayout = inflater.inflate(R.layout.popup_led, (ViewGroup) findViewById(R.id.popupLED));

        final TextView popupRText = (TextView) viewLayout.findViewById(R.id.popupRText);
        final TextView popupGText = (TextView) viewLayout.findViewById(R.id.popupGText);
        final TextView popupBText = (TextView) viewLayout.findViewById(R.id.popupBText);

        final ImageView popupPixel = (ImageView) viewLayout.findViewById(R.id.popupPixel);
        final ImageView setPixel = (ImageView) findViewById(pixelSelect);


        popDialog.setTitle("Select RGB values ");
        popDialog.setView(viewLayout);

        //  seekBarR
        final SeekBar seekR = (SeekBar) viewLayout.findViewById(R.id.seekBarR);
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
        final SeekBar seekG = (SeekBar) viewLayout.findViewById(R.id.seekBarG);
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
        final SeekBar seekB = (SeekBar) viewLayout.findViewById(R.id.seekBarB);
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

    public void showPresetList() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.popup_preset, (ViewGroup) findViewById(R.id.popupPreset));
        presetWindow = new PopupWindow(layout, 500, 400, true);
        presetWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);

        ImageButton btn_Preset1 = (ImageButton) layout.findViewById(R.id.btnPreset1);
        ImageButton btn_Preset2 = (ImageButton) layout.findViewById(R.id.btnPreset2);
        ImageButton btn_Preset3 = (ImageButton) layout.findViewById(R.id.btnPreset3);
        ImageButton btn_PresetPopupExit = (ImageButton) layout.findViewById(R.id.btnExit);

        btn_Preset1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset1();
                presetWindow.dismiss();
            }
        });

        btn_Preset2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset2();
                presetWindow.dismiss();
            }
        });

        btn_Preset3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setPreset3();
                presetWindow.dismiss();
            }
        });

        btn_PresetPopupExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                presetWindow.dismiss();
            }

        });

    }

    public void showTubeList() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.popup_tube, (ViewGroup) findViewById(R.id.popupTube));
        presetWindow = new PopupWindow(layout, 800, 600, true);
        presetWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);

        final ImageView tubeView = (ImageView) layout.findViewById(R.id.tubeView);

        Button btn_setTube1 = (Button) layout.findViewById(R.id.setTube1);
        Button btn_setTube2 = (Button) layout.findViewById(R.id.setTube2);
        Button btn_setTube3 = (Button) layout.findViewById(R.id.setTube3);
        Button btn_setTube4 = (Button) layout.findViewById(R.id.setTube4);
        Button btn_setTube5 = (Button) layout.findViewById(R.id.setTube5);

        final TextView textRValue = (TextView) layout.findViewById(R.id.textRValue);
        final TextView textGValue = (TextView) layout.findViewById(R.id.textGValue);
        final TextView textBValue = (TextView) layout.findViewById(R.id.textBValue);

        final SeekBar seekR = (SeekBar) layout.findViewById(R.id.seekBarR);
        final SeekBar seekG = (SeekBar) layout.findViewById(R.id.seekBarG);
        final SeekBar seekB = (SeekBar) layout.findViewById(R.id.seekBarB);

        seekR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                textRValue.setText("R: " + progress);
                rProgress = progress;

                tubeView.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        seekG.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                textGValue.setText("G: " + progress);
                gProgress = progress;

                tubeView.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        seekB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Do something here with new value
                textBValue.setText("B: " + progress);
                bProgress = progress;

                tubeView.setBackgroundColor(Color.rgb(rProgress, gProgress, bProgress));
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        btn_setTube1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setTube(1, rProgress, gProgress, bProgress);
                presetWindow.dismiss();
            }
        });

        btn_setTube2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setTube(2, rProgress, gProgress, bProgress);
                presetWindow.dismiss();
            }
        });

        btn_setTube3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setTube(3, rProgress, gProgress, bProgress);
                presetWindow.dismiss();
            }
        });

        btn_setTube4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setTube(4, rProgress, gProgress, bProgress);
                presetWindow.dismiss();
            }
        });

        btn_setTube5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setTube(5, rProgress, gProgress, bProgress);
                presetWindow.dismiss();
            }
        });
    }

    public void setTube(int tube, int rValue, int gValue, int bValue) {
        tubeSelect = tube;
        switch(tubeSelect){
            case 1:
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

                final ImageView strip1[] = {s1pixel1, s1pixel2, s1pixel3, s1pixel4,
                        s1pixel5, s1pixel6, s1pixel7, s1pixel8, s1pixel9, s1pixel10, s1pixel11 };

                for(int i = 0; i < STRIP_SIZE; i++) {
                    strip1[i].setBackgroundColor(Color.rgb(rValue, gValue, bValue));
                    s1[i][0] = (char) (rValue);
                    s1[i][1] = (char) (gValue);
                    s1[i][2] = (char) (bValue);
                }

                break;
            case 2:
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

                final ImageView strip2[] = {s2pixel1, s2pixel2, s2pixel3, s2pixel4,
                        s2pixel5, s2pixel6, s2pixel7, s2pixel8, s2pixel9, s2pixel10, s2pixel11 };

                for(int i = 0; i < STRIP_SIZE; i++) {
                    strip2[i].setBackgroundColor(Color.rgb(rValue, gValue, bValue));
                    s2[i][0] = (char) (rValue);
                    s2[i][1] = (char) (gValue);
                    s2[i][2] = (char) (bValue);
                }

                break;
            case 3:
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

                final ImageView strip3[] = {s3pixel1, s3pixel2, s3pixel3, s3pixel4,
                    s3pixel5, s3pixel6, s3pixel7, s3pixel8, s3pixel9, s3pixel10, s3pixel11};

                for(int i = 0; i < STRIP_SIZE; i++) {
                    strip3[i].setBackgroundColor(Color.rgb(rValue, gValue, bValue));
                    s3[i][0] = (char) (rValue);
                    s3[i][1] = (char) (gValue);
                    s3[i][2] = (char) (bValue);
                }

                break;
            case 4:
                break;
            case 5:
                break;
        }
    }

    public void setPreset1() {

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

        for(int i = 0; i < STRIP_SIZE; i++) {
           strip1[i].setBackgroundColor(Color.rgb(255, 255, 255));
           strip2[i].setBackgroundColor(Color.rgb(255, 255, 255));
           strip3[i].setBackgroundColor(Color.rgb(255, 255, 255));
            s1[i][0] = (char) (255);
            s1[i][1] = (char) (255);
            s1[i][2] = (char) (255);

            s2[i][0] = (char) (255);
            s2[i][1] = (char) (255);
            s2[i][2] = (char) (255);

            s3[i][0] = (char) (255);
            s3[i][1] = (char) (255);
            s3[i][2] = (char) (255);
       }

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

        for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
            strip1[cur_pixel].setBackgroundColor(Color.rgb(255, 0, 0));
            strips[0][cur_pixel][0] = (char) (255);
            strips[0][cur_pixel][1] = (char) (0);
            strips[0][cur_pixel][2] = (char) (0);
        }

        for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
            strip2[cur_pixel].setBackgroundColor(Color.rgb(0, 255, 0));
            strips[1][cur_pixel][0] = (char) (0);
            strips[1][cur_pixel][1] = (char) (255);
            strips[1][cur_pixel][2] = (char) (0);
        }

        for (int cur_pixel = 0; cur_pixel < STRIP_SIZE; cur_pixel++) {
            strip3[cur_pixel].setBackgroundColor(Color.rgb(0, 0, 255));
            strips[2][cur_pixel][0] = (char) (0);
            strips[2][cur_pixel][1] = (char) (0);
            strips[2][cur_pixel][2] = (char) (255);
        }

//        for(int i = 0; i < 6; i++) {
//            strip1[i].setBackgroundColor(Color.rgb(0, 255, 0));
//                s1[i][0] = (char) (0);
//                s1[i][1] = (char) (255);
//                s1[i][2] = (char) (0);
//            strip2[i].setBackgroundColor(Color.rgb(0, 255, 0));
//                s2[i][0] = (char) (0);
//                s2[i][1] = (char) (255);
//                s2[i][2] = (char) (0);
//            strip3[i].setBackgroundColor(Color.rgb(0, 255, 0));
//                s3[i][0] = (char) (0);
//                s3[i][1] = (char) (255);
//                s3[i][2] = (char) (0);
//        }
//        for(int i = 6; i < 9; i++) {
//            strip1[i].setBackgroundColor(Color.rgb(255, 255, 0));
//                s1[i][0] = (char) (255);
//                s1[i][1] = (char) (255);
//                s1[i][2] = (char) (0);
//            strip2[i].setBackgroundColor(Color.rgb(255, 255, 0));
//                s2[i][0] = (char) (255);
//                s2[i][1] = (char) (255);
//                s2[i][2] = (char) (0);
//            strip3[i].setBackgroundColor(Color.rgb(255, 255, 0));
//                s3[i][0] = (char) (255);
//                s3[i][1] = (char) (255);
//                s3[i][2] = (char) (0);
//        }
//        for(int i = 9; i < STRIP_SIZE; i++) {
//            strip1[i].setBackgroundColor(Color.rgb(255, 0, 0));
//                s1[i][0] = (char) (255);
//                s1[i][1] = (char) (0);
//                s1[i][2] = (char) (0);
//            strip2[i].setBackgroundColor(Color.rgb(255, 0, 0));
//                s2[i][0] = (char) (255);
//                s2[i][1] = (char) (0);
//                s2[i][2] = (char) (0);
//            strip3[i].setBackgroundColor(Color.rgb(255, 0, 0));
//                s3[i][0] = (char) (255);
//                s3[i][1] = (char) (0);
//                s3[i][2] = (char) (0);
//        }

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

        for(int i = 0; i < STRIP_SIZE; i++) {
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
