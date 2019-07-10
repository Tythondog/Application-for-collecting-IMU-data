package com.practice.cos;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.footaccgyr.SocketThread;

public class PracticeActivity extends Activity implements SensorEventListener, OnClickListener {
    /** Called when the activity is first created. */

    //设置LOG标签
    private Button mWriteButton, mStopButton, mShowButton, mConnectButton;
    private boolean doWrite = false;
    private SensorManager sm;
    private TextView AT,AT2,ACT, showFileNameTV, showFileTV;
    private EditText fileNameET;
    private EditText serverAddressET;
    private EditText serverPortET;

    private String server_address;
    private int server_port;

    private Socket socket;
    private SocketThread socketThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        AT = (TextView)findViewById(R.id.AT);
        AT2 = (TextView)findViewById(R.id.AT2);
        ACT = (TextView)findViewById(R.id.onAccuracyChanged);
        fileNameET = findViewById(R.id.File_Name);
        showFileTV = findViewById(R.id.Show_File_Content);
        showFileNameTV = findViewById(R.id.Show_File_Name);
        serverAddressET = findViewById(R.id.server_address);
        serverPortET = findViewById(R.id.server_port);

        //创建一个SensorManager来获取系统的传感器服务
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        /*
         * 最常用的一个方法 注册事件
         * 参数1 ：SensorEventListener监听器
         * 参数2 ：Sensor 一个服务可能有多个Sensor实现，此处调用getDefaultSensor获取默认的Sensor
         * 参数3 ：模式 可选数据变化的刷新频率
         * */

        try {
            String accFileName = "Accelerometer_" + fileNameET.getText() + ".txt";
            String scopeFileName = "Gyroscope_" + fileNameET.getText() + ".txt";

            openFileOutput(accFileName, Context.MODE_PRIVATE ).close();
            openFileOutput(scopeFileName, Context.MODE_PRIVATE).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mWriteButton = (Button) findViewById(R.id.Button_Write);
        mWriteButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.Button_Stop);
        mConnectButton = (Button)findViewById(R.id.Button_Connect);
        mStopButton.setOnClickListener(this);
        mShowButton = findViewById(R.id.Button_Show);
        mShowButton.setOnClickListener(this);
        mConnectButton.setOnClickListener(this);

        showFileTV.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        // 为系统的陀螺仪传感器注册监听器
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onStop()
    {
        // 程序退出时取消注册传感器监听器
        sm.unregisterListener(this);
        super.onStop();
    }



    public void onPause(){
        /*
         * 很关键的部分：注意，说明文档中提到，即使activity不可见的时候，感应器依然会继续的工作，测试的时候可以发现，没有正常的刷新频率
         * 也会非常高，所以一定要在onPause方法中关闭触发器，否则讲耗费用户大量电量，很不负责。
         * */
        sm.unregisterListener(this);
        super.onPause();
    }

    public void writeFileSdcard(String fileName,String message) {
        try {
            FileOutputStream fout = openFileOutput(fileName, Context.MODE_APPEND);
            byte [] bytes = message.getBytes();

            fout.write(bytes);
            fout.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ShowToast")
    public void connectServer(){
        Log.d(String.valueOf(R.string.app_name), "---------------------------");

        server_address = serverAddressET.getText().toString();
        server_port = Integer.valueOf(serverPortET.getText().toString());

        if (socketThread != null &&socketThread.isRun()) {
            Toast.makeText(PracticeActivity.this, "Connected", Toast.LENGTH_LONG).show();
            return;
        }

        socketThread = new SocketThread(server_address, server_port);
        socketThread.start();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.Button_Write) {
            doWrite = true;
        }
        if (v.getId() == R.id.Button_Stop) {
            doWrite = false;
        }
        if (v.getId() == R.id.Button_Connect) {
            connectServer();
        }
        if (v.getId() == R.id.Button_Show) {
            showFileTV.setText("");
            String accFileName = "Accelerometer_" + fileNameET.getText() + ".txt";
            showFileNameTV.setText(accFileName);
            try {
                InputStreamReader reader = new InputStreamReader(openFileInput(accFileName));
                char[] data = new char[1024];
                while (reader.read(data) != -1) {
                    showFileTV.append(String.valueOf(data));
                    Arrays.fill(data, (char)0);
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                showFileTV.setText("Failed to open file " + accFileName);
            } catch (IOException e) {
                e.printStackTrace();
                showFileTV.setText("Failed to read data from  " + accFileName);
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        ACT.setText("onAccuracyChanged被触发");
    }

    public static byte[] int2NB(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static byte[] short2NB(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) ( n       & 0xff);
        b[0] = (byte) ((n >> 8) & 0xff);
        return b;
    }

    public static byte[] char2NB(int n){
        byte[] b = new byte[1];
        b[0] = (byte) (n & 0xff);
        return b;
    }

    public byte[] byte_copy(byte[] des, byte[] src, int cur) {
        int len = src.length;
        for (int i = 0; i < len; ++i) {
            des[cur + i] = src[i];
        }
        return des;
    }

    public static byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;
    }

    public static byte[] long2Bytes(long num) {
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }


    public byte[] gen_header_data(int length, short size, int version, int reserved) {
        byte[] data = new byte[8];
        data = byte_copy(data, int2NB(length), 0);
        data = byte_copy(data, short2NB(size), 4);
        data = byte_copy(data, char2NB(version), 6);
        data = byte_copy(data, char2NB(reserved), 7);
        return data;
    }

    public byte[] gen_data(int time_stamp, float x, float y, float z, int type) {
        byte[] data = new byte[20];
        data = byte_copy(data, int2NB(time_stamp), 0);
        data = byte_copy(data, float2byte(x), 4);
        data = byte_copy(data, float2byte(y), 8);
        data = byte_copy(data, float2byte(z), 12);
        data = byte_copy(data, int2NB(type), 16);
        return data;
    }

    public void onSensorChanged(SensorEvent event) {
        String message = new String();
        String message2 = new String();
        String message3 = new String();
        DecimalFormat df = new DecimalFormat("#,##0.000");
        int sensorType = event.sensor.getType();
        int date = 0;
        switch (sensorType){
            case Sensor.TYPE_ACCELEROMETER:

                SimpleDateFormat sdf=new SimpleDateFormat("HHmmss");
                String str=sdf.format(new Date());
                date = Integer.valueOf(str);
                message=str +",";
                float X = event.values[0];
                float Y = event.values[1];
                float Z = event.values[2];

                message += df.format(X) + ",";
                message += df.format(Y) + ",";
                message += df.format(Z) + "\n";

                AT.setText(message + "\n");
                break;
            case Sensor.TYPE_GYROSCOPE:
                float X2 = event.values[0];
                float Y2 = event.values[1];
                float Z2 = event.values[2];

//                String time = message.substring(0,6);
                SimpleDateFormat lala=new SimpleDateFormat("HHmmss");
                String time = lala.format(new Date());
                date = Integer.valueOf(time);
                message2 = df.format(X2) + ",";
                message2 += df.format(Y2) + ",";
                message2 += df.format(Z2) +  "\n";
//                message += message2 + "\n";
//                message3 = time + message2 + "\n";
                message3 = time + "," + message2;
                AT2.setText(message2 + "\n");
                break;
        }

        if (doWrite) {
            String accFileName = "Accelerometer_" + fileNameET.getText() + ".txt";
            String scopeFileName = "Gyroscope_" + fileNameET.getText() + ".txt";

            Log.d("aaaa", String.format("Date: %d", date));
            Log.d("aaaa", String.valueOf(date));

            try {
                byte[] data = gen_data(date, event.values[0], event.values[1], event.values[2], sensorType);
                byte[] header_data = gen_header_data(8 + data.length, (short)1, 1, 0);


                socketThread.sendData(header_data);
                socketThread.sendData(data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            writeFileSdcard(accFileName, message);
            writeFileSdcard(scopeFileName, message3);
        }

    }
}