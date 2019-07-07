package cn.mcf.myapplication;

import android.util.Log;

/**
 * Created by McF on 2018/9/29.
 */

public class Self extends MapPointAndLine {
    private String account;
    private String password;

    //占据的车位序号，-1为未占据
    private int lotOccupying;
    public Self(){
        super();
        lotOccupying = -1;
    }

    public Self(float firstX,float firstY,int number){
        super(firstX,firstY,number);
        lotOccupying = -1;
    }
    public int getLotOccupying() {
        return lotOccupying;
    }

    public void setLotOccupying(int lotOccupying) {
        this.lotOccupying = lotOccupying;
    }



    //重置位置
    public void resetLoc(){
        int random = (int)(1+Math.random()*(8-1+1));
        Log.i("randomrandom",Integer.toString(random));
        switch(random){
            case 1:
                this.setViewX((float)(204+Math.random()*(1400-204+1)));
                this.setViewY(710);
                break;
            case 2:
                this.setViewX((float)(204+Math.random()*(1400-204+1)));
                this.setViewY(290);
                break;
            case 3:
                this.setViewX(1375);
                this.setViewY((float)(290+Math.random()*(520-290+1)));
                break;
            case 4:
                this.setViewX(1150);
                this.setViewY((float)(290+Math.random()*(710-290+1)));
                break;
            case 5:
                this.setViewX(910);
                this.setViewY((float)(290+Math.random()*(710-290+1)));
                break;
            case 6:
                this.setViewX(670);
                this.setViewY((float)(290+Math.random()*(710-290+1)));
                break;
            case 7:
                this.setViewX(450);
                this.setViewY((float)(290+Math.random()*(710-290+1)));
                break;
            case 8:
                this.setViewX(237);
                this.setViewY((float)(290+Math.random()*(710-290+1)));
                break;
        }
    }
}
