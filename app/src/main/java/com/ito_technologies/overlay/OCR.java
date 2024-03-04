package com.ito_technologies.overlay;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

public class OCR {
    private static OCR mOCR;
    private static ArrayList<Element> ocrResultList;
    public OCR(){
        mOCR = this;
    }
    public static OCR getInstance(){
        if( mOCR == null ){
            mOCR = new OCR();
        }
        return mOCR;
    }
    public void startOCR( Bitmap bitmap ){
        if( ocrResultList == null ){
            ocrResultList = new ArrayList<Element>();
        }
        if( ocrResultList != null ){
            ocrResultList.clear();
        }
        if( bitmap != null ){
            //recognizeText( FirebaseVisionImage.fromBitmap( bitmap ) );
            recognizeTextCloud( FirebaseVisionImage.fromBitmap( bitmap ) );
        }else{
            Log.i( "NoImage", "There is no image bitmap." );
        }
    }
    private void recognizeTextCloud(FirebaseVisionImage image) {
        // [START set_detector_options_cloud]
        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "ja"))
                .build();
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);

        // [START run_detector_cloud]
        Task<FirebaseVisionText> result = detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            Rect boundingBox = block.getBoundingBox();
                            Point[] cornerPoints = block.getCornerPoints();
                            String text = block.getText();

                            for (FirebaseVisionText.Line line: block.getLines()) {
                                // ...
                                Element element = new Element( line.getText(), line.getBoundingBox() );
                                ocrResultList.add(element);
                                FloatingViewService.floatingService.drawText( line );
//                                for (FirebaseVisionText.Element element: line.getElements()) {
//                                }
                            }
                        }
                        if( FloatingViewService.floatingService != null ){
                            FloatingViewService.floatingService.showProgressBar(false);
                        }
                        Gson gson = new Gson();
                        String s = gson.toJson( ocrResultList );
                        Log.i( "ResultInfo", s );
                        // [END get_text_cloud]
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.d( "OCR failed", e.getMessage() );
                        if( FloatingViewService.floatingService != null ){
                            FloatingViewService.floatingService.showProgressBar(false);
                        }
                    }
                });
        // [END run_detector_cloud]
    }
}

