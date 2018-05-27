package com.example.minhhuynh.face_detetion_load_image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int CAMERA_REQUEST = 2;
    private String mCurrentPhotoPath;
    private Bitmap myBitmap;
    ImageView imageView;
    Button btnProcess,btnUpLoad,btnTakePhoto,btnSave;

    Bitmap glass;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.image_view);
        btnProcess = (Button)findViewById(R.id.button_process);
        btnUpLoad = (Button)findViewById(R.id.button_load_image);
        btnTakePhoto =(Button)findViewById(R.id.button_photo);
        btnSave = (Button)findViewById(R.id.btn_Save);
        glass = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.glass1);

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.i(TAG, "IOException");
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    }
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSave();

            }
        });

        btnUpLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //imageView.setImageDrawable(null);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RESULT_LOAD_IMAGE);

            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBitmap == null){
                    Toast.makeText(MainActivity.this,
                            "Don't have Image",
                            Toast.LENGTH_LONG).show();
                }else{
                    detectFace();
                    Toast.makeText(MainActivity.this,
                            "Done",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            try {
                myBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                imageView.setImageBitmap(myBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == RESULT_LOAD_IMAGE
                && resultCode == RESULT_OK){

            if(myBitmap != null){
                myBitmap.recycle();
            }

            try {
                InputStream inputStream =
                        getContentResolver().openInputStream(data.getData());
                myBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                imageView.setImageBitmap(myBitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    private void startSave(){
        FileOutputStream fileOutputStream = null;
        File file = getDics();
        if(!file.exists() && !file.mkdir()){
            Toast.makeText(this,"Can't create directory to save image", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String name = "Img" + timeStamp + ".jpg";
        String file_name = file.getAbsolutePath() + "/" + name;
        File new_file = new File(file_name);

        try {
            fileOutputStream = new FileOutputStream(new_file);
            Bitmap bitmap = viewToBitmap(imageView,imageView.getWidth(),imageView.getHeight());
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
            Toast.makeText(this,"Save image success", Toast.LENGTH_SHORT).show();
            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshGalley(new_file);
    }
    private void refreshGalley(File file){
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
    }

    private File getDics() {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        return new File(file,"Image Demo");
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public static Bitmap viewToBitmap(View view,int width,int height){
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void detectFace(){

        //Create Rectangles
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(2);
        myRectPaint.setColor(Color.GREEN);
        myRectPaint.setStyle(Paint.Style.STROKE);

//        Paint landmarksPaint = new Paint();
//        landmarksPaint.setStrokeWidth(5);
//        landmarksPaint.setColor(Color.GREEN);
//        landmarksPaint.setStyle(Paint.Style.STROKE);

        //Create image
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //Detect the Faces
        FaceDetector faceDetector =
                new FaceDetector.Builder(getApplicationContext())
                        .setTrackingEnabled(false)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .build();

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        if(!faceDetector.isOperational()){
            Log.w(TAG,"Face detector could not be set up your device");
            Toast.makeText(MainActivity.this,"Face detector could not be set up your device", Toast.LENGTH_SHORT).show();
            return;
        }


        //Draw Rectangles on the Faces
        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            Log.v(TAG, "x1 = " + x1);//58
            Log.v(TAG, "y1 = " + y1);//35
            Log.v(TAG, "x2 = " + x2);//331
            Log.v(TAG, "y2 = " + y2);//377
            Log.v(TAG, "thisFace.getWidth()/2 = " + (int)thisFace.getWidth()/2); // 272
            Log.v(TAG, "thisFace.getHeight()/2 = " + (int)thisFace.getHeight()/2); // 340
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

            // Scale Glasses
            glass = Bitmap.createScaledBitmap(glass,(int)thisFace.getWidth()/2 + (int)thisFace.getWidth()/5,(int)thisFace.getHeight()/2 +(int)thisFace.getWidth()/5,false);

            //get Landmarks for the first face
//            List<Landmark> landmarks = thisFace.getLandmarks();
//            for(int j=0; j<landmarks.size(); j++){
//               PointF pos = landmarks.get(j).getPosition();
//                Log.v(TAG, "x = " + pos.x);
//                Log.v(TAG, "y = " + pos.y);
//               tempCanvas.drawPoint(pos.x, pos.y, landmarksPaint);
//            }

            for(Landmark landmark:thisFace.getLandmarks()) {
                int cx = (int) (landmark.getPosition().x);
                int cy = (int) (landmark.getPosition().y);
                if(landmark.getType() == Landmark.NOSE_BASE){

                    Log.v(TAG, "cx = " + cx);
                    Log.v(TAG, "cy = " + cy);
                    int scaleWidth = glass.getScaledWidth(tempCanvas);
                    int scaleHeight = glass.getScaledHeight(tempCanvas);
                    Log.v(TAG, "scaleWidth = " + glass.getScaledWidth(tempCanvas));//136
                    Log.v(TAG, "scaleHeight = " + glass.getScaledHeight(tempCanvas));//170
                    //tempCanvas.drawBitmap(glass, scaleWidth,scaleHeight - y1,null);//130 140

                    // Add Glasses
                    tempCanvas.drawBitmap(glass, cx-(scaleWidth/2),cy - scaleHeight + scaleHeight/4,null);
                    Log.v(TAG, "Width = " + (cx-(scaleWidth/2)));
                    Log.v(TAG, "Height = " + (cy - scaleHeight + scaleHeight/4));
                }
            }

        }

        imageView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));

    }


}
