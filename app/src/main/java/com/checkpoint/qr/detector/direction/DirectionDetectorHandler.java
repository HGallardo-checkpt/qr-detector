package com.checkpoint.qr.detector.direction;

import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import com.checkpoint.qr.detector.events.OnFinishDetectionEvent;
import com.checkpoint.qr.detector.events.OnPostProccessingNotDetectedEvent;
import com.checkpoint.qr.detector.pojos.DetectionResult;
import com.checkpoint.qr.detector.rest.QRDetectionEndPoint;

import java.util.List;

public class DirectionDetectorHandler  extends Handler {

    private static final String LOGTAG = DirectionDetectorHandler.class.getSimpleName();


    public DirectionDetectorHandler(final Looper looper) {
        super(looper);

    }


    @Override
    public void handleMessage(final android.os.Message msg) {
        final DirectionDetectorHandler.Message messageType = DirectionDetectorHandler.Message.fromOrdinal(msg.what);

        switch (messageType) {
            case CALCULATE_DIRECTION:
                Log.e("LAUNCH POSITION DETECTOR","---->");

                List<DirectionDetection> directionDetectionList = (List<DirectionDetection>) msg.obj;
                Log.e("----->",""+directionDetectionList.size());

                if(directionDetectionList.size()>0){
                  int size = directionDetectionList.size()-1;
                  Point center = directionDetectionList.get(0).center;
                  Point firstLocation = directionDetectionList.get(0).position;
                  Point secondLocation = directionDetectionList.get(size).position;
                  Log.e("center",""+center.toString());
                  Log.e("firstLocation",""+firstLocation.toString());
                  Log.e("secondLocation",""+secondLocation.toString());

                  if(firstLocation.x < secondLocation.x ){

                      new OnFinishDetectionEvent("DOWN TO UP",directionDetectionList.get(0).translate).broadcastEvent();

                  }
                  else if(firstLocation.x > secondLocation.x ){

                      new OnFinishDetectionEvent("UP to DOWN",directionDetectionList.get(0).translate).broadcastEvent();

                  }else{

                      if(secondLocation.x > center.x){

                          new OnFinishDetectionEvent("DOWN TO UP",directionDetectionList.get(0).translate).broadcastEvent();

                      }else{

                          new OnFinishDetectionEvent("UP to DOWN",directionDetectionList.get(0).translate).broadcastEvent();

                      }
                  }
                }


                break;
            default:
                Log.e(LOGTAG, "Unknown message received on DirectionDetectorHandlerThread");
        }
    }

    /**
     * Enum defining the messages that the BitmapHandlerThread can process.
     */
    public enum Message {
        CALCULATE_DIRECTION;
        private static final DirectionDetectorHandler.Message[] VALUES = values();
        private static DirectionDetectorHandler.Message fromOrdinal(final int ordinal) {
            return VALUES[ordinal];
        }
    }

}
