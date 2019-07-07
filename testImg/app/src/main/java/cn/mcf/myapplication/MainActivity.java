package cn.mcf.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //绘制路径和用户点的自定义控件
    private MyPointAndLineView myPointAndLineView;

    //绘制底层停车场地图的控件
    private ImageView map;

    //绘制停车位信息的自定义控件
    private MyLotView myLotView;

    //寻路按钮，点击后自动寻找最近车位并绘制路径
    private Button findPath;

    //刷新车位信息，便于测试本项目效果为给所有车位随机刷新状态（未连接数据库）
    private Button getLot;

    //模拟导航按钮
    private Button simulateGO;

    //随机重置用户位置按钮
    private Button resetLoc;

    //处理子线程信息的类
    private Handler myHandler;

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private RSSI rssiModule;


    float scale;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化
        map = this.findViewById(R.id.map);
        myLotView = this.findViewById(R.id.myLotView);
        myPointAndLineView = this.findViewById(R.id.myPointAndLineView);
        findPath = this.findViewById(R.id.findPath);
        getLot = this.findViewById(R.id.getLot);
        simulateGO = this.findViewById(R.id.simulateGO);
        resetLoc = this.findViewById(R.id.resetLoc);


        //蓝牙初始化
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "不支持蓝牙设备!", Toast.LENGTH_SHORT).show();
        }
        //map.setOnTouchListener(new TouchListener());

        myPointAndLineView.setMyLotView(myLotView);
        myPointAndLineView.init();

        //刷新页面
//        myLotView.invalidate();
//        myPointAndLineView.invalidate();

        //处理子线程信息的具体代码
        myHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                AlertDialog.Builder builder;
                switch(msg.what){
                    case MyPointAndLineView.SIMULATIONEND: //处理导航结束的反馈信息，弹窗提示
                        builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("提示");
                        builder.setMessage("导航结束");
                        builder.setPositiveButton("确定",null);
                        builder.show();
                        if(myPointAndLineView.getMe().getLotOccupying() == -1)
                            findPath.setText("寻找车位");
                        else
                            findPath.setText("寻车");
                        myPointAndLineView.setFindPathSignal(0);
                        myPointAndLineView.invalidate();
                        break;
                    case MyPointAndLineView.ALLOCCUPIED: //处理车位全满的反馈信息，弹窗提示
                        builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("提示");
                        builder.setMessage("该停车场已无空余车位");
                        builder.setPositiveButton("确定",null);
                        builder.show();
                        break;
                    case MyLotView.FRESHLOTOK:
                        myLotView.invalidate();
                        break;
                    case RSSI.NOBLUETOOTH:
                        Toast.makeText(getApplicationContext(), "不支持蓝牙设备!", Toast.LENGTH_SHORT).show();
                        break;
                    case RSSI.NOTOPENBLUETOOTH:
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        rssiModule.checkBlueTooth();
                        break;
                    case RSSI.BLUETOOTHOK:
                        rssiModule.testGet();

                        break;
                }
                super.handleMessage(msg);
            }
        };

        rssiModule = new RSSI(myHandler);
        myPointAndLineView.setMyHandler(myHandler);
        myLotView.setMyHandler(myHandler);
        //设置点击事件，实现相应按键功能
        findPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myPointAndLineView.findLot();
                myPointAndLineView.setFindPathSignal(1);
                myPointAndLineView.invalidate();
            }
        });


        simulateGO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myPointAndLineView.getFindPathSignal() == 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("提示");
                    builder.setMessage("请先寻找车位");
                    builder.setPositiveButton("确定",null);
                    builder.show();
                }
                else{
                    myPointAndLineView.simulateGO();
                }
            }
        });

        getLot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myPointAndLineView.resetLot();
                myPointAndLineView.getMe().setLotOccupying(-1);
                //myLotView.invalidate();
                myPointAndLineView.setFindPathSignal(0);
                myPointAndLineView.invalidate();
            }
        });

        resetLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myPointAndLineView.getMe().resetLoc();
                myPointAndLineView.setFindPathSignal(0);
                myPointAndLineView.invalidate();
            }
        });
    }

    private class TouchListener implements View.OnTouchListener {

        int length = 68;int halfLength = 34;//车位轮廓在地图上长68单位
        int width = 28;int halfWidth = 14;//宽28单位

        /** 记录是拖拉照片模式还是放大缩小照片模式 */
        private int mode = 0;// 初始状态
        /** 拖拉照片模式 */
        private static final int MODE_DRAG = 1;
        /** 放大缩小照片模式 */
        private static final int MODE_ZOOM = 2;

        /** 用于记录开始时候的坐标位置 */
        private PointF startPoint = new PointF();
        /** 用于记录拖拉图片移动的坐标位置 */
        private Matrix matrix = new Matrix();
        private Matrix lotsMatrix = new Matrix();
        /** 用于记录图片要进行拖拉时候的坐标位置 */
        private Matrix currentMatrix = new Matrix();

        private Matrix currentLotsMatrix = new Matrix();

        /** 两个手指的开始距离 */
        private float startDis;
        /** 两个手指的中间点 */
        private PointF midPoint;
        /** 放大倍数 */
        @Override
        public boolean onTouch(View v, MotionEvent event){
            /** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
            switch(event.getAction() & MotionEvent.ACTION_MASK) {
                //手指下压屏幕
                case MotionEvent.ACTION_DOWN:
                    mode = MODE_DRAG;
                    //记录ImageView当前移动位置
                    currentMatrix.set(map.getImageMatrix());
                    currentLotsMatrix.set(myLotView.getImageMatrix());
                    startPoint.set(event.getX(), event.getY());
                    break;
                // 手指在屏幕上移动，该事件会被不断触发
                case MotionEvent.ACTION_MOVE:
                    //拖拉图片
                    if (mode == MODE_DRAG) {
                        float dx = event.getX() - startPoint.x;
                        float dy = event.getY() - startPoint.y;
                        //在没有移动之前的位置上进行移动
                        //Log.i("drag_test","drag");
                        matrix.set(currentMatrix);
                        matrix.postTranslate(dx, dy);
                        lotsMatrix.set(currentLotsMatrix);
                        lotsMatrix.postTranslate(dx,dy);
//                        for(MyRectF lot : lots){
//                            lot.set(lot.left - startPoint.x,lot.top - startPoint.y,lot.right-startPoint.x,lot.bottom-startPoint.y);
//                        }
//                        myLotView.invalidate();

                    }
                    //放大缩小图片
                    else if (mode == MODE_ZOOM) {
                        float endDis = distance(event);//结束距离
                        //Log.i("zoom_test","zoom");
                        if (endDis > 10f) {
                            scale = endDis / startDis;
                            matrix.set(currentMatrix);
                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        }
                    }
                    break;
                //手指离开屏幕
                case MotionEvent.ACTION_UP:
                    // 当触点离开屏幕，但是屏幕上还有触点(手指)
                case MotionEvent.ACTION_POINTER_UP:
                    mode = 0;
                    break;
                // 当屏幕上已经有触点(手指)，再有一个触点压下屏幕
                case MotionEvent.ACTION_POINTER_DOWN:
                    //Log.i("mode_drag","mode_drag");
                    mode = MODE_ZOOM;
                    /** 计算两个手指间的距离 */
                    startDis = distance(event);
                    if (startDis > 10f) { // 两个手指并拢在一起的时候像素大于10
                        midPoint = mid(event);
                        //记录当前ImageView的缩放倍数
                        currentMatrix.set(map.getImageMatrix());
                    }
                    break;

            }
            map.setImageMatrix(matrix);
            myLotView.setImageMatrix(lotsMatrix);
            return true;
        }
        /** 计算两个手指间的距离 */
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            /** 使用勾股定理返回两点之间的距离 */
            return (float)Math.sqrt(dx * dx + dy * dy);
        }

        /** 计算两个手指间的中间点 */
        private PointF mid(MotionEvent event) {
            float midX = (event.getX(1) + event.getX(0)) / 2;
            float midY = (event.getY(1) + event.getY(0)) / 2;
            return new PointF(midX, midY);
        }
    }
}
