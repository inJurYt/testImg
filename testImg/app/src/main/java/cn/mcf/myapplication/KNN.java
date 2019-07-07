package cn.mcf.myapplication;

import java.util.ArrayList;

public class KNN {
    private int nowRSSI;
    private ArrayList<Location> locations;

    public KNN(int nowRSSI,ArrayList<Location> locations){
        this.nowRSSI = nowRSSI;
        this.locations = locations;
    }

    public Location getKNN(){
        Location result = new Location(0,0,0);
        int gapMin = Math.abs(result.getRSSI() - nowRSSI);
        for(Location loc : locations){
            int gap = Math.abs(loc.getRSSI() - nowRSSI);
            if(gap < gapMin){
                gapMin = gap;
                result = loc;
            }
        }
        return result;
    }
}
