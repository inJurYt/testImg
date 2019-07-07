package cn.mcf.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.lang.Thread;

/**
 * Created by McF on 2018/9/13.
 */

public class MyPointAndLineView extends AppCompatImageView {
    //所有导航点
    private ArrayList<MapPointAndLine> mapPointAndLines;

    //存储A*算法中每个选取的点的父节点
    private int[] chosenPoints;

    //用户点
    private Self me;

    //目的地车位序号
    private int destination;

    //目的地车位对应的导航点序号
    private int endNumber;

    //模拟导航中用户点预计到达的下一个导航点的序号
    private int first;

    //是否已经得出路径信息的信号，绘图时使用
    private int findPathSignal;

    //车位视图类，连接导航点和车位时使用
    private MyLotView myLotView;

    //发送反馈信息的目的地
    private Handler myHandler;

    public static final int SIMULATIONEND = 1;//导航结束信号
    public static final int ALLOCCUPIED = 2;//车位全满信号


    public MyPointAndLineView(Context context){
        super(context);
        mapPointAndLines = new ArrayList<>();
        me = new Self();
//        init();
        destination = 0;
//       findLot();
    }

    public MyPointAndLineView(Context context, AttributeSet attrs){
        super(context,attrs);
        mapPointAndLines = new ArrayList<>();
        me = new Self(1150,400,0);
//        init();
        destination = 0;
//        findLot();
    }


    public Self getMe() {
        return me;
    }

    public void setMyLotView(MyLotView myLotView) {
        this.myLotView = myLotView;
    }

    public void setMyHandler(Handler myHandler) {
        this.myHandler = myHandler;
    }

    public void setFindPathSignal(int signal){
        this.findPathSignal = signal;
    }

    public int getFindPathSignal() {
        return findPathSignal;
    }

    //重置车位信息
    public void resetLot(){
//        for(MyRectF lot:myLotView.getLots())
//            lot.resetState();
        myLotView.getParkLots();
    }

    //模拟导航，实现逻辑为 若用户点与终点的坐标差不在5个单位以内，将用户点的X、Y坐标分别向下一个路径点靠近2单位，并重新绘制路径
    public void simulateGO(){
        new Thread(myRunnable).start();
    }

    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            while (Math.abs(me.getViewX() - mapPointAndLines.get(endNumber - 1).getViewX()) > 5 || Math.abs(me.getViewY() - mapPointAndLines.get(endNumber - 1).getViewY()) >5 ) {
                Log.i("checkcheck",Float.toString(me.getViewX())+" "+Float.toString(me.getViewY()));
                for(MapPointAndLine next:me.getNext())
                    Log.i("checkcheck",Integer.toString(next.getNumber()));
                try {
                    while(chosenPoints == null)
                        Thread.sleep(100);
                    if(Math.abs(me.getViewX() - mapPointAndLines.get(first - 1).getViewX()) >0)
                        me.setViewX(me.getViewX() + (-2) * (me.getViewX() - mapPointAndLines.get(first - 1).getViewX()) / Math.abs(me.getViewX() - mapPointAndLines.get(first - 1).getViewX()));
                    if(Math.abs(me.getViewY() - mapPointAndLines.get(first - 1).getViewY()) >0)
                        me.setViewY(me.getViewY() + (-2) * (me.getViewY() - mapPointAndLines.get(first - 1).getViewY()) / Math.abs(me.getViewY() - mapPointAndLines.get(first - 1).getViewY()));
                    postInvalidate();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(me.getLotOccupying() == -1){
                me.setLotOccupying(destination);
                myLotView.getLots().get(destination - 1).setState(MyRectF.OCCUPIED);
            }
            else{
                me.setLotOccupying(-1);
                myLotView.getLots().get(destination - 1).setState(MyRectF.AVAILABLE);
            }
            Message msg = Message.obtain(myHandler);
            msg.what = SIMULATIONEND;
            msg.sendToTarget();
        }
    };

    //A*公式 f(n) = g(n) + h(n)
    //计算h(n)
    public int calH(MapPointAndLine self,MapPointAndLine end){
        return (int)Math.abs(self.getViewX() - end.getViewX() + self.getViewY() - end.getViewY());
    }

    //计算 g(n) 和 g(n+1) 的差值
    public int calDg(MapPointAndLine parent,MapPointAndLine self){
        return (int)Math.abs(self.getViewX() - parent.getViewX() + self.getViewY() - parent.getViewY());
    }

    //A*算法 初始激活点为用户点，每次选取激活点中f(n)最小的，将其加入选取点列表，并将它的邻接点加入激活点列表，周而复始直至终点加入选取点列表
    public void AStar(int[] parent,ArrayList<MapPointAndLine> checking,int[] fForStar,int[] gForStar,MapPointAndLine end){
        while(checking.size() != 0){
            MapPointAndLine best = checking.get(0);
            for(MapPointAndLine inCheck:checking){
                if(fForStar[inCheck.getNumber()] < fForStar[best.getNumber()]){
                    best = inCheck;
                }
            }
            checking.remove(best);
            if(best == end){
                return;
            }
            for(MapPointAndLine bestNext:best.getNext()) {
                if(fForStar[bestNext.getNumber()] == 0) {
                    gForStar[bestNext.getNumber()] = gForStar[best.getNumber()]+calDg(best,bestNext);
                    fForStar[bestNext.getNumber()] = gForStar[bestNext.getNumber()] + calH(bestNext, end);
                    checking.add(bestNext);
                    parent[bestNext.getNumber()] = best.getNumber();
                }
            }
        }
        return;
    }

    //跟距用户点当前位置配置用户点的邻接点 选取原则为：选取上下左右方向各一个距离最近的点
    public void findNearestOfMe(){
        if(me.getNext().size() > 0)
            me.getNext().clear();
        for(MapPointAndLine near:mapPointAndLines){
            if(Math.abs(near.getViewX() - me.getViewX()) < 5 && Math.abs(near.getViewY() - me.getViewY()) < 168){
                MapPointAndLine nearestY = new MapPointAndLine(-1,-1,-1);
                float disYIn,disYOut;
                disYIn = 0;
                disYOut = me.getViewY() - near.getViewY();
                if(disYOut > 0)
                {
                    for(MapPointAndLine findNearestYOfMe:me.getNext()){
                        if(Math.abs(findNearestYOfMe.getViewX() - me.getViewX()) <5 ) {
                            disYIn = me.getViewY() - findNearestYOfMe.getViewY();
                            if(disYIn > 0)
                                nearestY = findNearestYOfMe;
                        }
                    }
                    if(nearestY.getNumber() == -1)
                        me.getNext().add(near);
                    else if(disYIn > disYOut){
                        me.getNext().remove(nearestY);
                        me.getNext().add(near);
                    }
                }
                else if(disYOut < 0){
                    for(MapPointAndLine findNearestYOfMe:me.getNext()){
                        if(Math.abs(findNearestYOfMe.getViewX() - me.getViewX()) <5 ) {
                            disYIn = me.getViewY() - findNearestYOfMe.getViewY();
                            if(disYIn < 0)
                                nearestY = findNearestYOfMe;
                        }
                    }
                    if(nearestY.getNumber() == -1)
                        me.getNext().add(near);
                    else if(disYIn < disYOut){
                        me.getNext().remove(nearestY);
                        me.getNext().add(near);
                    }
                }
            }
            else if(Math.abs(near.getViewY() - me.getViewY()) < 5 && Math.abs(near.getViewX() - me.getViewX()) < 168){
                MapPointAndLine nearestX = new MapPointAndLine(-1,-1,-1);
                float disXIn,disXOut;
                disXIn = 0;
                disXOut = me.getViewX() - near.getViewX();
                if(disXOut > 0)
                {
                    for(MapPointAndLine findNearestXOfMe:me.getNext()){
                        if(Math.abs(findNearestXOfMe.getViewY() - me.getViewY()) <5 ) {
                            disXIn = me.getViewX() - findNearestXOfMe.getViewX();
                            if(disXIn > 0)
                                nearestX = findNearestXOfMe;
                        }
                    }
                    if(nearestX.getNumber() == -1)
                        me.getNext().add(near);
                    else if(disXIn > disXOut){
                        me.getNext().remove(nearestX);
                        me.getNext().add(near);
                    }
                }
                else if(disXOut < 0){
                    for(MapPointAndLine findNearestXOfMe:me.getNext()){
                        if(Math.abs(findNearestXOfMe.getViewY() - me.getViewY()) <5 ) {
                            disXIn = me.getViewX() - findNearestXOfMe.getViewX();
                            if(disXIn < 0)
                                nearestX = findNearestXOfMe;
                        }
                    }
                    if(nearestX.getNumber() == -1)
                        me.getNext().add(near);
                    else if(disXIn < disXOut){
                        me.getNext().remove(nearestX);
                        me.getNext().add(near);
                    }
                }
            }
        }
    }

    //将上述函数整合实现路径规划，参数为终点导航点序号
    public void findPath(int number){
        int[] fForAStar = new int[114];//f(n)数组
        int[] gForAStar = new int[114];//g(n)数组
        int[] parent = new int[114];//父节点数组
        ArrayList<MapPointAndLine> checking = new ArrayList<>();
        checking.add(me);
        gForAStar[me.getNumber()] = 0;
        fForAStar[me.getNumber()] = calH(me,mapPointAndLines.get(number - 1));
        findNearestOfMe();
        AStar(parent,checking,fForAStar,gForAStar,mapPointAndLines.get(number - 1));
        chosenPoints = parent;
    }

    public void findPath(int number,ArrayList<Integer> availableLotsDaoHang){
        int[] parent = new int[114];//父节点数组
        Queue<MapPointAndLine> checking = new LinkedList<>();
        checking.offer(me);
        findNearestOfMe();
        BFS(parent,checking,availableLotsDaoHang);
        chosenPoints = parent;
    }

    public void BFS(int[] parent,Queue<MapPointAndLine> checking,ArrayList<Integer> availableLotsDaoHang){
        while(checking.size() != 0){
            MapPointAndLine mrf = checking.poll();
            if(availableLotsDaoHang.contains(mrf.getNumber())){
                endNumber = mrf.getNumber();
                for(MyRectF des : myLotView.getLots()){
                    if(des.getMapPoint() == endNumber && des.getState() == MyRectF.AVAILABLE){
                        destination = des.getNumber();
                        break;
                    }
                }
                return;
            }
            for(MapPointAndLine next: mrf.getNext()){
                checking.offer(next);
                parent[next.getNumber()] = mrf.getNumber();
            }
        }
        return;
    }

    //寻找最近的空闲车位
    public void findLot(){
        if(me.getLotOccupying() != -1){
            findPath(endNumber);
            return;
        }
        destination = 0;
        endNumber = 0;
        float distance = 0;
//        for(MyRectF lot:myLotView.getLots()){
//            if(lot.getState() == MyRectF.AVAILABLE){
//                if(destination == 0){
//                    destination = lot.getNumber();
//                    endNumber = lot.getMapPoint();
//                    distance = Math.abs(me.getViewX() - mapPointAndLines.get(lot.getMapPoint() - 1).getViewX()) +
//                            Math.abs(me.getViewY() - mapPointAndLines.get(lot.getMapPoint() - 1).getViewY());
//                }
//                else{
//                    float checkDistance = Math.abs(me.getViewX() - mapPointAndLines.get(lot.getMapPoint() - 1).getViewX()) +
//                            Math.abs(me.getViewY() - mapPointAndLines.get(lot.getMapPoint() - 1).getViewY());
//                    if(checkDistance < distance){
//                        destination = lot.getNumber();
//                        endNumber = lot.getMapPoint();
//                        distance = checkDistance;
//                    }
//                }
//            }
//        }
        ArrayList<Integer> availableLotsDaoHang = new ArrayList<>();
        for(MyRectF lot:myLotView.getLots()){
            if(lot.getState() == MyRectF.AVAILABLE)
                availableLotsDaoHang.add(lot.getMapPoint());
        }

        if(availableLotsDaoHang.size() == 0){
            Message msg =Message.obtain(myHandler);
            msg.what = ALLOCCUPIED;
            msg.sendToTarget();
        }
        else {
            findPath(endNumber,availableLotsDaoHang);
        }
    }

    //初始化，写入各导航点和各车位在地图上的坐标，配置各自的邻接点
    public void init(){
        int number = 1;int lotNumber = 1;//分别为导航点序号和车位序号
        int length = 68;int halfLength = 34;//车位轮廓在地图上长68单位
        int width = 28;int halfWidth = 14;//宽28单位
        mapPointAndLines.add(new MapPointAndLine(1400,710,number));
        myLotView.getLots().add(new MyRectF(1400 - halfWidth,757,1400 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1372,710,number));
        myLotView.getLots().add(new MyRectF(1372 - halfWidth,757,1372 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1344,710,number));
        myLotView.getLots().add(new MyRectF(1344 - halfWidth,757,1344 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1310,710,number));
        myLotView.getLots().add(new MyRectF(1310 - halfWidth,757,1310 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1282,710,number));
        myLotView.getLots().add(new MyRectF(1282 - halfWidth,757,1282 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1254,710,number));
        myLotView.getLots().add(new MyRectF(1254 - halfWidth,757,1254 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1226,710,number));
        myLotView.getLots().add(new MyRectF(1226 - halfWidth,757,1226 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1192,710,number));
        myLotView.getLots().add(new MyRectF(1192 - halfWidth,757,1192 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1164,710,number));
        myLotView.getLots().add(new MyRectF(1164 - halfWidth,757,1164 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,710,number));number++;//拐点 9
        mapPointAndLines.add(new MapPointAndLine(1136,710,number));
        myLotView.getLots().add(new MyRectF(1136 - halfWidth,757,1136 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1108,710,number));
        myLotView.getLots().add(new MyRectF(1108 - halfWidth,757,1108 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1074,710,number));
        myLotView.getLots().add(new MyRectF(1074 - halfWidth,757,1074 + halfWidth,757 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,710,number));number++;//拐点 13
        mapPointAndLines.add(new MapPointAndLine(903,710,number));
        myLotView.getLots().add(new MyRectF(903 - halfWidth,740,903 + halfWidth,740 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(875,710,number));
        myLotView.getLots().add(new MyRectF(875 - halfWidth,740,875 + halfWidth,740 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(832,710,number));
        myLotView.getLots().add(new MyRectF(832 - halfWidth,740,832 + halfWidth,740 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(804,710,number));
        myLotView.getLots().add(new MyRectF(804 - halfWidth,740,804 + halfWidth,740 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(670,710,number));number++;//拐点 18
        mapPointAndLines.add(new MapPointAndLine(640,710,number));
        myLotView.getLots().add(new MyRectF(640 - halfWidth,737,640 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(610,710,number));
        myLotView.getLots().add(new MyRectF(610 - halfWidth,737,610 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(582,710,number));
        myLotView.getLots().add(new MyRectF(582 - halfWidth,737,582 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(554,710,number));
        myLotView.getLots().add(new MyRectF(554 - halfWidth,737,554 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(526,710,number));
        myLotView.getLots().add(new MyRectF(526 - halfWidth,737,526 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(491,710,number));
        myLotView.getLots().add(new MyRectF(491 - halfWidth,737,491 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(463,710,number));
        myLotView.getLots().add(new MyRectF(463 - halfWidth,737,463 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(450,710,number));number++;//拐点 26
        mapPointAndLines.add(new MapPointAndLine(435,710,number));
        myLotView.getLots().add(new MyRectF(435 - halfWidth,737,435 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(407,710,number));
        myLotView.getLots().add(new MyRectF(407 - halfWidth,737,407 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(374,710,number));
        myLotView.getLots().add(new MyRectF(374 - halfWidth,737,374 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(346,710,number));
        myLotView.getLots().add(new MyRectF(346 - halfWidth,737,346 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(318,710,number));
        myLotView.getLots().add(new MyRectF(318 - halfWidth,737,318 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(290,710,number));
        myLotView.getLots().add(new MyRectF(290 - halfWidth,737,290 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(260,710,number));
        myLotView.getLots().add(new MyRectF(260 - halfWidth,737,260 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(237,710,number));number++;//拐点 34
        mapPointAndLines.add(new MapPointAndLine(232,710,number));
        myLotView.getLots().add(new MyRectF(232 - halfWidth,737,232 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(204,710,number));//36
        myLotView.getLots().add(new MyRectF(204 - halfWidth,737,204 + halfWidth,737 + length , lotNumber,number));number++;lotNumber++;

        mapPointAndLines.add(new MapPointAndLine(1375,520,number));number++;//拐点 37
        mapPointAndLines.add(new MapPointAndLine(1375,455,number));
        myLotView.getLots().add(new MyRectF(1270,455 - halfWidth,1270 + length,455 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1375,423,number));
        myLotView.getLots().add(new MyRectF(1270,423 - halfWidth,1270 + length,423 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1375,395,number));
        myLotView.getLots().add(new MyRectF(1270,395 - halfWidth,1270 + length,395 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1375,367,number));
        myLotView.getLots().add(new MyRectF(1270,367 - halfWidth,1270 + length,367 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1375,339,number));
        myLotView.getLots().add(new MyRectF(1270,339 - halfWidth,1270 + length,339 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1375,290,number));number++;//拐点 43

        mapPointAndLines.add(new MapPointAndLine(1150,645,number));
        myLotView.getLots().add(new MyRectF(1202,645 - halfWidth,1202 + length,645 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,645 - halfWidth,1028 + length,645 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,617,number));
        myLotView.getLots().add(new MyRectF(1202,617 - halfWidth,1202 + length,617 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,617 - halfWidth,1028 + length,617 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,589,number));
        myLotView.getLots().add(new MyRectF(1202,589 - halfWidth,1202 + length,589 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,589 - halfWidth,1028 + length,589 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,525,number));
        myLotView.getLots().add(new MyRectF(1028,525 - halfWidth,1028 + length,525 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,520,number));number++;//拐点 48
        mapPointAndLines.add(new MapPointAndLine(1150,497,number));
        myLotView.getLots().add(new MyRectF(1028,497 - halfWidth,1028 + length,497 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,469,number));
        myLotView.getLots().add(new MyRectF(1028,469 - halfWidth,1028 + length,469 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,455,number));
        myLotView.getLots().add(new MyRectF(1202,455 - halfWidth,1202 + length,455 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,423,number));
        myLotView.getLots().add(new MyRectF(1202,423 - halfWidth,1202 + length,423 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,423 - halfWidth,1028 + length,423 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,395,number));
        myLotView.getLots().add(new MyRectF(1202,395 - halfWidth,1202 + length,395 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,395 - halfWidth,1028 + length,395 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,367,number));
        myLotView.getLots().add(new MyRectF(1202,367 - halfWidth,1202 + length,367 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,367 - halfWidth,1028 + length,367 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,339,number));
        myLotView.getLots().add(new MyRectF(1202,339 - halfWidth,1202 + length,339 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(1028,339 - halfWidth,1028 + length,339 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(1150,290,number));number++;//拐点 56
        mapPointAndLines.add(new MapPointAndLine(1285,520,number));//57
        myLotView.getLots().add(new MyRectF(1272,573,1272 + width,573 + length , lotNumber,number));number++;lotNumber++;

        mapPointAndLines.add(new MapPointAndLine(910,645,number));
        myLotView.getLots().add(new MyRectF(958,645 - halfWidth,958 + length,645 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,617,number));
        myLotView.getLots().add(new MyRectF(958,617 - halfWidth,958 + length,617 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,617 - halfWidth,782 + length,617 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,589,number));
        myLotView.getLots().add(new MyRectF(958,589 - halfWidth,958 + length,589 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,589 - halfWidth,782 + length,589 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,525,number));
        myLotView.getLots().add(new MyRectF(958,525 - halfWidth,958 + length,525 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,525 - halfWidth,782 + length,525 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,497,number));
        myLotView.getLots().add(new MyRectF(958,497 - halfWidth,958 + length,497 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,497 - halfWidth,782 + length,497 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,469,number));
        myLotView.getLots().add(new MyRectF(958,469 - halfWidth,958 + length,469 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,469 - halfWidth,782 + length,469 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,410,number));
        myLotView.getLots().add(new MyRectF(958,410 - halfWidth,958 + length,410 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,410 - halfWidth,782 + length,410 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,382,number));
        myLotView.getLots().add(new MyRectF(958,382 - halfWidth,958 + length,382 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,382 - halfWidth,782 + length,382 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,354,number));
        myLotView.getLots().add(new MyRectF(958,354 - halfWidth,958 + length,354 + halfWidth , lotNumber,number));lotNumber++;
        myLotView.getLots().add(new MyRectF(782,354 - halfWidth,782 + length,354 + halfWidth , lotNumber,number));number++;lotNumber++;
        mapPointAndLines.add(new MapPointAndLine(910,290,number));number++;//拐点 67
        for(int i = 0;i < 3; i++){
            int x = 0;int lotX1 = 0;int lotX2 = 0;
            if(i == 0){
                x = 670;
                lotX1 = 710;
                lotX2 = 563;
            }
            else if(i == 1){
                x = 450;
                lotX1 = 494;
                lotX2 = 340;
            }
            else{
                x = 237;
                lotX1 = 270;
                lotX2 = 144;
            }
            mapPointAndLines.add(new MapPointAndLine(x,655,number));//68 81 94
            myLotView.getLots().add(new MyRectF(lotX1,655 - halfWidth,lotX1 + length,655 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,655 - halfWidth,lotX2 + length,655 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,627,number));
            myLotView.getLots().add(new MyRectF(lotX1,627 - halfWidth,lotX1 + length,627 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,627 - halfWidth,lotX2 + length,627 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,599,number));
            myLotView.getLots().add(new MyRectF(lotX1,599 - halfWidth,lotX1 + length,599 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,599 - halfWidth,lotX2 + length,599 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,571,number));
            myLotView.getLots().add(new MyRectF(lotX1,571 - halfWidth,lotX1 + length,571 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,571 - halfWidth,lotX2 + length,571 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,541,number));
            myLotView.getLots().add(new MyRectF(lotX1,541 - halfWidth,lotX1 + length,541 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,541 - halfWidth,lotX2 + length,541 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,513,number));
            myLotView.getLots().add(new MyRectF(lotX1,513 - halfWidth,lotX1 + length,513 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,513 - halfWidth,lotX2 + length,513 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,485,number));
            myLotView.getLots().add(new MyRectF(lotX1,485 - halfWidth,lotX1 + length,485 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,485 - halfWidth,lotX2 + length,485 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,457,number));
            myLotView.getLots().add(new MyRectF(lotX1,457 - halfWidth,lotX1 + length,457 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,457 - halfWidth,lotX2 + length,457 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,422,number));
            myLotView.getLots().add(new MyRectF(lotX1,422 - halfWidth,lotX1 + length,422 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,422 - halfWidth,lotX2 + length,422 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,394,number));
            myLotView.getLots().add(new MyRectF(lotX1,394 - halfWidth,lotX1 + length,394 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,394 - halfWidth,lotX2 + length,394 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,366,number));
            myLotView.getLots().add(new MyRectF(lotX1,366 - halfWidth,lotX1 + length,366 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,366 - halfWidth,lotX2 + length,366 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,338,number));
            myLotView.getLots().add(new MyRectF(lotX1,338 - halfWidth,lotX1 + length,338 + halfWidth , lotNumber,number));lotNumber++;
            myLotView.getLots().add(new MyRectF(lotX2,338 - halfWidth,lotX2 + length,338 + halfWidth , lotNumber,number));number++;lotNumber++;
            mapPointAndLines.add(new MapPointAndLine(x,290,number));number++;//拐点 80 93 106
        }

        myLotView.getLots().add(new MyRectF(144,308-halfWidth,144 + length,308 + halfWidth,lotNumber,number - 1));
        mapPointAndLines.add(new MapPointAndLine(237,303,number));number++;//107

        mapPointAndLines.add(new MapPointAndLine(1263,290,number));number++;//108
        mapPointAndLines.add(new MapPointAndLine(1030,290,number));number++;//109
        mapPointAndLines.add(new MapPointAndLine(790,290,number));number++;//110
        mapPointAndLines.add(new MapPointAndLine(560,290,number));number++;//111
        mapPointAndLines.add(new MapPointAndLine(343,290,number));number++;//112

        Log.d("allLots",String.valueOf(lotNumber));

        //配置邻接表
        int i = 0;
        for(i = 0 ;i < 36 ; i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 37;i < 43; i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 44;i < 56; i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 58;i < 67;i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 68;i < 80;i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 81;i < 93;i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        for(i = 94;i < 106;i++){
            mapPointAndLines.get(i).addNext(mapPointAndLines.get(i+1));
            mapPointAndLines.get(i+1).addNext(mapPointAndLines.get(i));
        }
        mapPointAndLines.get(105).addNext(mapPointAndLines.get(107));
        mapPointAndLines.get(107).addNext(mapPointAndLines.get(105));
        mapPointAndLines.get(107).addNext(mapPointAndLines.get(106));
        mapPointAndLines.get(106).addNext(mapPointAndLines.get(107));

        mapPointAndLines.get(9).addNext(mapPointAndLines.get(44));
        mapPointAndLines.get(44).addNext(mapPointAndLines.get(9));
        mapPointAndLines.get(13).addNext(mapPointAndLines.get(58));
        mapPointAndLines.get(58).addNext(mapPointAndLines.get(13));
        mapPointAndLines.get(18).addNext(mapPointAndLines.get(68));
        mapPointAndLines.get(68).addNext(mapPointAndLines.get(18));
        mapPointAndLines.get(26).addNext(mapPointAndLines.get(81));
        mapPointAndLines.get(81).addNext(mapPointAndLines.get(26));
        mapPointAndLines.get(34).addNext(mapPointAndLines.get(94));
        mapPointAndLines.get(94).addNext(mapPointAndLines.get(34));
//        mapPointAndLines.get(56).addNext(mapPointAndLines.get(67));
//        mapPointAndLines.get(67).addNext(mapPointAndLines.get(56));
//        mapPointAndLines.get(56).addNext(mapPointAndLines.get(43));
//        mapPointAndLines.get(43).addNext(mapPointAndLines.get(56));
//        mapPointAndLines.get(67).addNext(mapPointAndLines.get(80));
//        mapPointAndLines.get(80).addNext(mapPointAndLines.get(67));
//        mapPointAndLines.get(80).addNext(mapPointAndLines.get(93));
//        mapPointAndLines.get(93).addNext(mapPointAndLines.get(80));
//        mapPointAndLines.get(93).addNext(mapPointAndLines.get(106));
//        mapPointAndLines.get(106).addNext(mapPointAndLines.get(93));
        mapPointAndLines.get(37).addNext(mapPointAndLines.get(57));
        mapPointAndLines.get(57).addNext(mapPointAndLines.get(37));
        mapPointAndLines.get(57).addNext(mapPointAndLines.get(48));
        mapPointAndLines.get(48).addNext(mapPointAndLines.get(57));

        mapPointAndLines.get(43).addNext(mapPointAndLines.get(108));
        mapPointAndLines.get(108).addNext(mapPointAndLines.get(43));
        mapPointAndLines.get(108).addNext(mapPointAndLines.get(56));
        mapPointAndLines.get(56).addNext(mapPointAndLines.get(108));
        mapPointAndLines.get(56).addNext(mapPointAndLines.get(109));
        mapPointAndLines.get(109).addNext(mapPointAndLines.get(56));
        mapPointAndLines.get(109).addNext(mapPointAndLines.get(67));
        mapPointAndLines.get(67).addNext(mapPointAndLines.get(109));
        mapPointAndLines.get(67).addNext(mapPointAndLines.get(110));
        mapPointAndLines.get(110).addNext(mapPointAndLines.get(67));
        mapPointAndLines.get(110).addNext(mapPointAndLines.get(80));
        mapPointAndLines.get(80).addNext(mapPointAndLines.get(110));
        mapPointAndLines.get(80).addNext(mapPointAndLines.get(111));
        mapPointAndLines.get(111).addNext(mapPointAndLines.get(80));
        mapPointAndLines.get(111).addNext(mapPointAndLines.get(93));
        mapPointAndLines.get(93).addNext(mapPointAndLines.get(111));
        mapPointAndLines.get(93).addNext(mapPointAndLines.get(112));
        mapPointAndLines.get(112).addNext(mapPointAndLines.get(93));
        mapPointAndLines.get(112).addNext(mapPointAndLines.get(106));
        mapPointAndLines.get(106).addNext(mapPointAndLines.get(112));

        myLotView.getParkLots();
    }


    //绘制路径和用户点
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStrokeWidth((float)5.0);
        paint.setColor(Color.RED);
        if(findPathSignal == 1){
            //从终点开始倒推绘制路线
            if(destination != 0) {
                findPath(endNumber);
                int now = endNumber;
                while (chosenPoints[now] != 0) {
                    canvas.drawLine(mapPointAndLines.get(now - 1).getViewX(), mapPointAndLines.get(now - 1).getViewY(),
                            mapPointAndLines.get(chosenPoints[now] - 1).getViewX(), mapPointAndLines.get(chosenPoints[now] - 1).getViewY(), paint);
                    now = chosenPoints[now];
                }
                first = now;
                //绘制连接用户点与第一个路径导航点的直线
                canvas.drawLine(me.getViewX(), me.getViewY(), mapPointAndLines.get(now - 1).getViewX(), mapPointAndLines.get(now - 1).getViewY(), paint);

                //绘制连接终点导航点和对应的车位的直线
                canvas.drawLine(myLotView.getLots().get(destination - 1).centerX(), myLotView.getLots().get(destination - 1).centerY(), mapPointAndLines.get(endNumber - 1).getViewX(), mapPointAndLines.get(endNumber - 1).getViewY(), paint);
            }
        }
        if(me.getLotOccupying() != -1){
            canvas.drawCircle(myLotView.getLots().get(me.getLotOccupying() - 1).centerX(),myLotView.getLots().get(me.getLotOccupying() - 1).centerY(),10,paint);
        }
        canvas.drawCircle(me.getViewX(),me.getViewY(),5,paint);
    }
}
