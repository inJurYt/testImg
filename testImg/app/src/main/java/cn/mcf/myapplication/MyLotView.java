package cn.mcf.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedOutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by McF on 2018/9/25.
 */

public class MyLotView extends AppCompatImageView {
    //停车场内的所有车位
    private ArrayList<MyRectF> lots;
    private Handler myHandler;
    public static final int FRESHLOTOK = 3;

    private class lotJson{
        String area_no;
        String area_name;
        List<Integer> data;
    }

    private class lotJsonAli{
        int _id;
        boolean isAvailable;
    }

    public MyLotView(Context context){
        super(context);
        lots = new ArrayList<>();
    }

    public MyLotView(Context context, AttributeSet attrs){
        super(context,attrs);
        lots = new ArrayList<>();
    }


    public void setMyHandler(Handler myHandler) {
        this.myHandler = myHandler;
    }

    public ArrayList<MyRectF> getLots() {
        return lots;
    }

    public void setLots(ArrayList<MyRectF> lots) {
        this.lots = lots;
    }
    //绘制车位信息
    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Paint paint =new Paint();
        paint.setStrokeWidth((float)5.0);
        paint.setColor(Color.RED);
        for(MyRectF lot:lots){
            if(lot.getState() == MyRectF.OCCUPIED){
                //从res中选取车辆贴图，若车位状态为 占据 ，则画上贴图
                Bitmap car = BitmapFactory.decodeResource(getResources(),R.mipmap.car);
                Matrix matrix = new Matrix();
                if(lot.getNumber() >= 33)
                    matrix.postRotate(90);
                else
                    matrix.postRotate(0);
                Bitmap carToDraw = Bitmap.createBitmap(car, 0, 0, car.getWidth(), car.getHeight(),
                        matrix, true);
                // 在画布上绘制旋转后的位图，有些车位水平，有些竖直
                canvas.drawBitmap(carToDraw,null,lot,paint);
            }
        }
    }

    public void getParkLots(){
        new Thread(myRunnable).start();

    }

    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://" + netInf.ipAli + ':' + netInf.portAli + "/getParkLots?parkID=park1")
                        .build();
                Response response = null;
                response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    Gson gson = new Gson();
                    String lotsArray = response.body().string();
                    List<lotJsonAli> occupiedLots = gson.fromJson(lotsArray, new TypeToken<List<lotJsonAli>>(){}.getType());
                    for(lotJsonAli i : occupiedLots){
                        if(i.isAvailable == true){
                            lots.get(i._id - 1).setState(MyRectF.AVAILABLE);
                        }
                        else
                            lots.get(i._id - 1).setState(MyRectF.OCCUPIED);
                    }
                }
//                if(response.isSuccessful()){
//                    Gson gson = new Gson();
//                    String resStr = response.body().string();
//                    lotJson lot = gson.fromJson(resStr,lotJson.class);
//                    for(int i = 0; i < lot.data.size();i++){
//                        if(lot.data.get(i) == 2){
//                            lots.get(i).setState(MyRectF.AVAILABLE);
//                        }
//                        else if(lot.data.get(i) == 3){
//                            lots.get(i).setState(MyRectF.OCCUPIED);
//                        }
//                        else{
//                            lots.get(i).setState(MyRectF.UNKNOWN);
//                        }
//                    }
//                }
                Message msg =Message.obtain(myHandler);
                msg.what = FRESHLOTOK;
                msg.sendToTarget();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    };
}
