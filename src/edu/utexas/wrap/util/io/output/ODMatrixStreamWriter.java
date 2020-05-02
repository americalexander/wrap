package edu.utexas.wrap.util.io.output;

import edu.utexas.wrap.demand.ODMatrix;
import edu.utexas.wrap.net.TravelSurveyZone;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static edu.utexas.wrap.wrapNCTCOG.printTimeStamp;


public class ODMatrixStreamWriter {


    public static void write(String timePeriod, Collection<ODMatrix> ods, OutputStream stdin) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
            List<ODMatrix> temp = new ArrayList(ods);
            // Assuming that there is something in the ods
            if (temp.size() == 0) {
                System.out.println("ODs has nothing...");
                System.exit(1);
            }
            BufferedOutputStream bo = new BufferedOutputStream(stdin);
            Collection<TravelSurveyZone> origins = temp.get(0).getGraph().getTSZs();
            Collection<TravelSurveyZone> demands = temp.get(0).getGraph().getTSZs();
//            System.out.println("There are " + origins.size() + " zones");
            int count = 0;
            for(TravelSurveyZone orig : origins) {
                count += 1;
                for(TravelSurveyZone dest : demands) {
                    buffer.clear();
                    buffer.putInt(orig.getNode().getID());
                    buffer.putInt(dest.getNode().getID());
                    Map<String, Float> od_info = new HashMap<>();
                    for (ODMatrix od : ods) {
                        float demand = od.getDemand(orig, dest);
                        od_info.put(od.getMode() + "_" + od.getVOT(), demand);
                    }
//                    System.out.println("-------------------------------------");
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("SINGLE_OCC_0.35")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("SINGLE_OCC_0.9")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("HOV_0.35")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("HOV_0.9")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("SINGLE_OCC_0.17")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("SINGLE_OCC_0.45")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("HOV_0.17")));
                    buffer.putInt(Float.floatToRawIntBits(od_info.get("HOV_0.45")));
                    // MED_TRUCKS and HEAVY_TRUCKS
                    buffer.putFloat(Float.floatToRawIntBits(0f));
                    buffer.putFloat(Float.floatToRawIntBits(0f));
//                    System.out.println("r:" + orig.getNode().getID() + " s:" + dest.getNode().getID() + " "
//                        + od_info.get("SINGLE_OCC_0.35") + " " + od_info.get("SINGLE_OCC_0.9")
//                        + od_info.get("HOV_0.35") + " " + od_info.get("HOV_0.9")
//                        + od_info.get("SINGLE_OCC_0.17") + " " + od_info.get("SINGLE_OCC_0.45")
//                        + od_info.get("HOV_0.17") + " " + od_info.get("HOV_0.45")
////                            + od_info.get("SINGLE_OCC_0.35") + " " + od_info.get("SINGLE_OCC_0.9")
//                    );
                    try {
                        buffer.flip();
                        bo.write(buffer.array());
//                        stdin.flush();
                    } catch (IOException e) {
                        if(e.toString().contains("Broken Pipe")) {
                            System.err.println("TAP-B has an error");
                        }
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                printTimeStamp();
                System.out.println(timePeriod + ":Finished " + count + "/"+ origins.size() +" zones so far");
                bo.flush();
//                System.out.println("=============================================");
            }
            bo.write("Sto".getBytes());
            bo.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}