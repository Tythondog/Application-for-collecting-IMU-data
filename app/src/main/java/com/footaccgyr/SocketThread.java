package com.footaccgyr;

import android.util.Log;

import com.practice.cos.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SocketThread extends Thread {

    private String server_address_;
    private Integer server_port_;

    private boolean stop_;
    private Socket socket_;

    private BlockingQueue<byte[]> queue;

    public SocketThread(String address, Integer port) {
        server_address_ = address;
        server_port_ = port;
        stop_ = false;
        queue = new LinkedBlockingDeque<>();
    }

    public synchronized void sendData(byte[] data) throws InterruptedException {
        queue.put(data);
    }

    public synchronized void stopServer(){
        stop_ = true;
    }

    public synchronized boolean isRun() {
        return !stop_;
    }

    @Override
    public void run() {
        Log.d("aaaa", "Thread running ...");
        OutputStream outputStream;
        try {
            socket_ = new Socket(server_address_, server_port_);
            outputStream = socket_.getOutputStream();
            Log.d(String.valueOf(R.string.app_name), String.format("Connected to %s:%d ...", server_address_, server_port_));
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("aaaa", "Failed to connected to server");
            return;
        }

        while (!stop_) {
            try {
                byte[] data;
                synchronized (queue){
                    data = queue.take();
                }

                if (data.length != 0) {
                    outputStream.write(data);
                    Log.d("aaaa", String.format("Write %d bytes data", data.length));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("aaaa", "Failed to send data to server");
            }
        }

    }
}
