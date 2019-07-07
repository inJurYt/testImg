package cn.mcf.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class RSSI {
    private ArrayList<String> rssiArr;//一次的各信标的rssi值
    private ArrayList<String> nameArr; //保存信标的地址（name）
    private ArrayList<Integer> totalRssiArr;//各信标20次的rssi总值
    private ArrayList<Integer> scanNumArr;//扫描次数，总扫描某信标的次数
    private String res = null;//用来显示
    private String send_date = null;//发送给服务器
    private BroadcastReceiver mReceiver;
    private Handler myHandler;
    public static final int NOBLUETOOTH = 4;
    public static final int NOTOPENBLUETOOTH = 5;
    public static final int BLUETOOTHOK = 6;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView textView;

    public RSSI(Handler myHandler){
        this.myHandler = myHandler;
        //蓝牙初始化
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //Toast.makeText(this, "不支持蓝牙设备!", Toast.LENGTH_SHORT).show();
            Message msg =Message.obtain(myHandler);
            msg.what = NOBLUETOOTH;
            msg.sendToTarget();
        }
        else {
            init_beacon();
            checkBlueTooth();
        }
    }



    private void init_beacon() {
        nameArr = new ArrayList<String>();
        nameArr.add("EWD14A1A5E3");//增加信标
        nameArr.add("EWE7823236F");
        nameArr.add("EWCA5F5B3B9");

        rssiArr = new ArrayList<String>();//距离由rssi获取
        rssiArr.add(null);
        rssiArr.add(null);
        rssiArr.add(null);

        totalRssiArr = new ArrayList<Integer>();
        totalRssiArr.add(0);
        totalRssiArr.add(0);
        totalRssiArr.add(0);

        scanNumArr = new ArrayList<Integer>();
        scanNumArr.add(0);
        scanNumArr.add(0);
        scanNumArr.add(0);
    }

    public void checkBlueTooth(){
        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Message msg =Message.obtain(myHandler);
            msg.what = NOTOPENBLUETOOTH;
            msg.sendToTarget();
        }
        else{
            Message msg =Message.obtain(myHandler);
            msg.what = BLUETOOTHOK;
            msg.sendToTarget();
        }
    }

    public void testGet(){
        mReceiver = new BroadcastReceiver() {
            int scanInRound = 0;
            int newestScanCount = 0;
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("BlueToothaction",action);
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (scanDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                        for (int i = 0; i < nameArr.size(); i++) {
                            if (nameArr.get(i).equals(scanDevice.getName())) {
                                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                                rssiArr.set(i,String.valueOf(rssi));
                                totalRssiArr.set(i,totalRssiArr.get(i) + Integer.valueOf(rssi));
                                newestScanCount = scanNumArr.get(i) + 1;
                                scanNumArr.set(i,newestScanCount);
                                String str = textView.getText().toString() + '\n' + scanDevice.getName() + "   " + rssi;
                                textView.setText(str);
                                scanInRound++;
                            }
                        }
//                        if(newestScanCount >= 20){
//                            mBluetoothAdapter.cancelDiscovery();
//                            String str1 = textView.getText().toString() + '\n' + "取指纹结束";
//                            textView.setText(str1);
//                            for (int i = 0; i < nameArr.size(); i++) {
//                                res += "name:" + nameArr.get(i) + " " + "rssi均值:" + totalRssiArr.get(i) / Double.valueOf(totalRssiArr.get(i)) + " " + "检测次数：" + scanNumArr.get(i) + '\n';
//                            }
//                            String str2 = textView.getText().toString() + '\n' + res;
//                            textView.setText(str2);
//                        }
                        if(scanInRound >= 6){
                            mBluetoothAdapter.cancelDiscovery();
                            String str1 = textView.getText().toString() + '\n' + "取指纹结束";
                            textView.setText(str1);
                            for (int i = 0; i < nameArr.size(); i++) {
                                res += "name:" + nameArr.get(i) + " " + "rssi均值:" + totalRssiArr.get(i) / Double.valueOf(scanNumArr.get(i)) + " " + "检测次数：" + scanNumArr.get(i) + '\n';
                            }
                            String str2 = textView.getText().toString() + '\n' + res;
                            textView.setText(str2);
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // 注册广播接收器，接收并处理搜索结果
        //registerReceiver(mReceiver, intentFilter);
        mBluetoothAdapter.startDiscovery();
    }
}
