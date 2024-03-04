package com.ito_technologies.overlay;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;

public class OcrActivity extends AppCompatActivity {

    ImageView imageView = null;
    Button btn = null;
    Point oPoint = new Point();
    Point fPoint = new Point();

    ArrayList scalePoint = new ArrayList<Double>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_ocr);
        btn = findViewById(R.id.notify_me);
        imageView = findViewById( R.id.img_view );
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                oPoint.x = imageView.getWidth();
                oPoint.y = imageView.getHeight();

                if( bitmap != null ){
                    //recognizeText( FirebaseVisionImage.fromBitmap( bitmap ) );
                    recognizeTextCloud( FirebaseVisionImage.fromBitmap( bitmap ) );
                }else{
                    Log.i( "NoImage", "There is no image bitmap." );
                }
            }
        });
    }
    private void scale(){
        scalePoint.add( (oPoint.x * 1.0 /fPoint.x ) ) ;
        scalePoint.add( (oPoint.y * 1.0 /fPoint.y ) ) ;
    }
    private void recognizeText(FirebaseVisionImage image) {
        fPoint.x = image.getBitmap().getWidth();
        fPoint.y = image.getBitmap().getHeight();
        scale();
        // [START get_detector_default]
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // [END get_detector_default]

        // [START run_detector]
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                // [START_EXCLUDE]
                                // [START get_text]
                                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                                    Rect boundingBox = block.getBoundingBox();
                                    Point[] cornerPoints = block.getCornerPoints();
                                    String text = block.getText();

                                    for (FirebaseVisionText.Line line: block.getLines()) {
                                        // ...
                                        drawRect( line );
                                        for (FirebaseVisionText.Element element: line.getElements()) {
                                            // ...
                                        }
                                    }
                                }
                                // [END get_text]
                                // [END_EXCLUDE]
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
Log.i( "OCR", e.getMessage() );
                                    }
                                });
        // [END run_detector]
    }

    private void drawRect( FirebaseVisionText.Line element ){
Log.i( "Element", "Start" );
//        ImageView image = new ImageView (this);
////        image.setLayoutParams (new RelativeLayout.LayoutParams(200, 200));
//        ShapeDrawable badge = new ShapeDrawable(new RectShape());
//        badge.setIntrinsicWidth (200);
//        badge.setIntrinsicHeight (200);
//        badge.getPaint().setColor(Color.RED);
//        image.setImageDrawable (badge);
//        image.setLeft( 200 );
//        image.setTop(200);
//        for( int i = 0; i < element.getCornerPoints().length; i ++ ){

            //Point pt = element.getCornerPoints()[i];
            Rect rect = element.getBoundingBox();
            CustomDrawableView v = new CustomDrawableView(this, rect, element.getText(), scalePoint );
            this.addContentView( v, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) );
//        }
    }
    private void recognizeTextCloud(FirebaseVisionImage image) {
        // [START set_detector_options_cloud]
        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "ja"))
                .build();
        // [END set_detector_options_cloud]

        // [START get_detector_cloud]
//        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
//                .getCloudTextRecognizer();
        // Or, to change the default settings:
           FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                  .getCloudTextRecognizer(options);
        // [END get_detector_cloud]

        // [START run_detector_cloud]
        Task<FirebaseVisionText> result = detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        // [START_EXCLUDE]
                        // [START get_text_cloud]
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            Rect boundingBox = block.getBoundingBox();
                            Point[] cornerPoints = block.getCornerPoints();
                            String text = block.getText();

                            for (FirebaseVisionText.Line line: block.getLines()) {
                                // ...
                                for (FirebaseVisionText.Element element: line.getElements()) {
                                    // ...
                                }
                            }
                        }
                        // [END get_text_cloud]
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
Log.d( "OCR failed", e.getMessage() );
                        // ...
                    }
                });
        // [END run_detector_cloud]
    }
}