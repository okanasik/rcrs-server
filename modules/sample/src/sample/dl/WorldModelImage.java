package sample.dl;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by okan on 14.03.2018.
 */
public class WorldModelImage {

    //    protected int minNodeDist = 0;
    protected List<List<byte[][]>> imagesList;
    protected List<Integer> remainingWaterList;

    protected int width;
    protected int height;

    protected int offsetx;
    protected int offsety;

    protected double xStep;
    protected double yStep;

    public WorldModelImage() {
        imagesList = new ArrayList<>();
    }

    public void calGrid(/*WorldInfo worldInfo */) {
        Collection<EntityID> entityIDs = null; //worldInfo.getEntityIDsOfType(StandardEntityURN.BUILDING,
//                StandardEntityURN.HYDRANT, StandardEntityURN.ROAD, StandardEntityURN.REFUGE,
//                StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE);

//        minNodeDist = Integer.MAX_VALUE;
        //RoboAKUTTimeUtil.startTime("image_dist");
//        EntityID[] entityIDArray = new EntityID[entityIDs.size()];
//        entityIDs.toArray(entityIDArray);

        double avgNodeDist = 0.0;

//        for (int i = 0; i < entityIDArray.length; i++) {
//            for (int j = i+1; j < entityIDArray.length; j++) {
//                int dist = worldInfo.getDistance(entityIDArray[i], entityIDArray[j]);
//                if (dist < minNodeDist) {
//                    minNodeDist = dist;
//                }
//            }
//        }

        double minAvgNodeDist = 0.0;
        int counter = 0;

//        for (int i = 0; i < entityIDArray.length; i++) {
//            minAvgNodeDist = Double.MAX_VALUE;
//            for (int j = 0; j < entityIDArray.length; j++) {
//                if (i == j) {
//                    continue;
//                }
//                int dist = worldInfo.getDistance(entityIDArray[i], entityIDArray[j]);
//                if (dist < minAvgNodeDist) {
//                    minAvgNodeDist = dist;
//                }
//            }
//            avgNodeDist += minAvgNodeDist;
//            counter++;
//        }

        width = 480;
        height = 480;


        Pair<Pair<Integer, Integer>, Pair<Integer,Integer>> worldBounds = null; //worldInfo.getWorldBounds();
        int worldWidth = worldBounds.second().first() - worldBounds.first().first();
        int worldHeight = worldBounds.second().second() - worldBounds.first().second();
        System.out.println("world.width:" + worldWidth + " world.height:" + worldHeight);
        xStep = worldWidth / width;
        yStep = worldHeight / height;
        System.out.println("xstep:" + xStep + " ystep:" + yStep);

        offsetx = worldBounds.first().first();
        offsety = worldBounds.first().second();
        System.out.println("offsetx:" + offsetx + " offsety:" + offsety);
    }

    public byte[][] calBuildingSizeImage(/*WorldInfo worldInfo*/) {

        Collection<StandardEntity> entities = null; //worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,
                //StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE);

        double[] buildingSizes = new double[entities.size()];
        double maxBuildingSize = Integer.MIN_VALUE;
        int i = 0;
        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                int size = ((Building)entity).getTotalArea();
                buildingSizes[i] = size;
                if (size > maxBuildingSize) {
                    maxBuildingSize = size;
                }
            }
            i++;
        }
        for (i = 0; i < buildingSizes.length; i++) {
            buildingSizes[i] = Math.floor((buildingSizes[i] / maxBuildingSize)*255);
        }

        byte[][] imageData = new byte[height][];
        for (int y = 0; y < height; y++) {
            imageData[y] = new byte[width];
        }

        i = 0;
        for (StandardEntity entity : entities) {
            //Pair<Integer, Integer> loc = worldInfo.getLocation(entity.getID());
            Pair<Integer, Integer> loc = new Pair<>(0,0);
            int x = (int)Math.floor((loc.first() - offsetx) / xStep);
            int y = (int)Math.floor((loc.second() - offsety) / yStep);
            imageData[y][x] = (byte)buildingSizes[i];
            i++;
        }

        /*
        System.out.println("image start");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(imageData[y][x] & 0xFF);
            }
            System.out.println("");
        }
        System.out.println("image end");
        */

        return imageData;
    }

    public byte[][] calTemperatureImage(/* WorldInfo worldInfo */) {
//        Collection<Building> buildingsOnFire = worldInfo.getFireBuildings();
        Collection<Building> buildingsOnFire = new ArrayList<>();
        int[] temperatures = new int[buildingsOnFire.size()];
        int i = 0;
        Integer maxTemp = Integer.MIN_VALUE;
        for (Building b : buildingsOnFire) {
            if (b.isTemperatureDefined()) {
                temperatures[i] = b.getTemperature();
                if (b.getTemperature() > maxTemp) {
                    maxTemp = b.getTemperature();
                }
            } else {
                temperatures[i] = 0;
            }
            i++;
        }

        for (i = 0; i < temperatures.length; i++) {
            temperatures[i] = (int)Math.floor((temperatures[i] / (1.0*maxTemp))*255);
        }

        byte[][] imageData = new byte[height][];
        for (int y = 0; y < height; y++) {
            imageData[y] = new byte[width];
        }

        i = 0;
        for (Building b : buildingsOnFire) {
//            Pair<Integer, Integer> loc = worldInfo.getLocation(b.getID());
            Pair<Integer, Integer> loc = null;
            int x = (int)Math.floor((loc.first() - offsetx) / xStep);
            int y = (int)Math.floor((loc.second() - offsety) / yStep);
            imageData[y][x] = (byte)temperatures[i];
            i++;
        }

        return imageData;
    }

    public byte[][] calFierynessImage(/*WorldInfo worldInfo */) {
//        Collection<Building> buildingsOnFire = worldInfo.getFireBuildings();
        Collection<Building> buildingsOnFire = null;
        int[] fieryness = new int[buildingsOnFire.size()];
        int i = 0;
        Integer maxFieryness = Integer.MIN_VALUE;
        for (Building b : buildingsOnFire) {
            if (b.isFierynessDefined()) {
                fieryness[i] = b.getFieryness();
                if (b.getTemperature() > maxFieryness) {
                    maxFieryness = b.getFieryness();
                }
            } else {
                fieryness[i] = 0;
            }
            i++;
        }

        for (i = 0; i < fieryness.length; i++) {
            fieryness[i] = (int)Math.floor((fieryness[i] / (1.0*maxFieryness))*255);
        }

        byte[][] imageData = new byte[height][];
        for (int y = 0; y < height; y++) {
            imageData[y] = new byte[width];
        }

        i = 0;
        for (Building b : buildingsOnFire) {
//            Pair<Integer, Integer> loc = worldInfo.getLocation(b.getID());
            Pair<Integer, Integer> loc = null;
            int x = (int)Math.floor((loc.first() - offsetx) / xStep);
            int y = (int)Math.floor((loc.second() - offsety) / yStep);
            imageData[y][x] = (byte)fieryness[i];
            i++;
        }

        return imageData;
    }

    public byte[][] agentPositionImage(/*WorldInfo worldInfo, AgentInfo agentInfo*/) {
        byte[][] imageData = new byte[height][];
        for (int y = 0; y < height; y++) {
            imageData[y] = new byte[width];
        }

//        System.out.println("agent.pos.x:" + agentInfo.getX() + " grid.y:" + agentInfo.getX());
//        int x = (int)Math.floor((agentInfo.getX() - offsetx) / xStep);
//        int y = (int)Math.floor((agentInfo.getY() - offsety) / yStep);
//        System.out.println("agent.grid.x:" + x + " grid.y:" + y);
//        imageData[y][x] = (byte)255;
        return imageData;
    }

    public byte[][] teammatePositionImage(/* WorldInfo worldInfo, AgentInfo agentInfo */ ) {
        byte[][] imageData = new byte[height][];
        for (int y = 0; y < height; y++) {
            imageData[y] = new byte[width];
        }

//        Collection<EntityID> entityIDS = worldInfo.getEntityIDsOfType(agentInfo.me().getStandardURN());
        Collection<EntityID> entityIDS = null;
        for (EntityID entityID : entityIDS) {
//            Pair<Integer, Integer> pos = worldInfo.getLocation(entityID);
            Pair<Integer, Integer> pos = null;
            int x = (int)Math.floor((pos.first() - offsetx) / xStep);
            int y = (int)Math.floor((pos.second() - offsety) / yStep);
            imageData[y][x] = (byte)255;
        }

        return imageData;
    }

    public void calculateImages(/*WorldInfo worldInfo, AgentInfo agentInfo*/) {
        List<byte[][]> images = new ArrayList<>();
        byte[][] image = null; //calBuildingSizeImage(worldInfo);
        images.add(image);
//        image = calTemperatureImage(worldInfo);
//        images.add(image);
//        image = calFierynessImage(worldInfo);
//        images.add(image);
//        image = agentPositionImage(worldInfo, agentInfo);
//        images.add(image);
//        image = teammatePositionImage(worldInfo, agentInfo);
//        images.add(image);
        imagesList.add(images);

//        if (agentInfo.me().getStandardURN() == StandardEntityURN.FIRE_BRIGADE) {
//            remainingWaterList.add(agentInfo.getWater());
//        }
    }

    public void saveData(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.close();
        }
        catch (Exception ex) {

        }
    }

    public void writeImage(ByteArrayOutputStream bos, byte[][] imageData) {
        try {
            bos.write(ByteBuffer.allocate(4).putInt(imageData.length).array());
            bos.write(ByteBuffer.allocate(4).putInt(imageData[0].length).array());
            for (int y = 0; y < imageData.length; y++) {
                for (int x = 0; x < imageData[y].length; x++) {
                    bos.write(imageData[y][x]);
                }
            }
        } catch (Exception ex) {

        }
    }

//    public int getMinNodeDist() {
//        return minNodeDist;
//    }

}
