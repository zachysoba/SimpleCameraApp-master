package com.dlsu.getbetter.cameraapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("opencv", "opencv initialization failed");
        } else {
            Log.i("opencv", "opencv initialization successful");
        }
    }

    private static final int IMAGE_CAPTURE = 1;
    ImageView imageView;
    Uri fileUri;
    Uri fileUri2;
    private static final String TAG = "OpenCV:Error";
    CascadeClassifier face_cascade;
    CascadeClassifier eyes_cascade;
    Button editButton1;
    Button editButton2;
    Button saveButton;
    Button getFiles;
    Button viewFiles;

    int nFaces;
    int nEyes;
    int xMin[][] = new int[20][20];
    int yMin[][] = new int[20][20];
    int xMax[][] = new int[20][20];
    int yMax[][] = new int[20][20];
    Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = (TextView) findViewById(R.id.hello1);
        Button button = (Button) findViewById(R.id.button1);
        imageView = (ImageView)findViewById(R.id.image1);
        editButton1 = (Button) findViewById(R.id.button2);
        editButton2 = (Button) findViewById(R.id.button3);
        saveButton = (Button) findViewById(R.id.button4);
        getFiles = (Button) findViewById(R.id.getFiles);
        viewFiles = (Button) findViewById(R.id.viewFiles);

        getFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Processing has started!", Toast.LENGTH_LONG).show();
                getFiles();
            }
        });

        final MainActivity m = this;

        viewFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(m, ProcessedImagesView.class);
                startActivity(intent);
            }
        });

        editButton1.setVisibility(View.GONE);
        editButton2.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        editButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(m, ImageEdit.class);
                i.putExtra("image", fileUri.getPath());
                i.putExtra("image2", fileUri2.getPath());
                startActivityForResult(i, 90);
            }
        });

        editButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(m, ImageEdit.class);
                i.putExtra("image", fileUri.getPath());
                i.putExtra("image2", fileUri.getPath());
                startActivityForResult(i, 90);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File disregard = new File(fileUri2.getPath());
                boolean deleted = disregard.delete();
                imageView.setImageBitmap(null);
                editButton1.setVisibility(View.GONE);
                editButton2.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
            }
        });

        setTitle("GetBetter");

        button.setOnClickListener(this);



    }

    @Override
    public void onClick(View v) {

        takePicture();

    }

    public void getFiles() {
        CensorCrawledImages c = new CensorCrawledImages();
        c.execute();
//        c.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void applyCensor(final int requestCode, final int resultCode, Intent data) {
        try {
            Bitmap duplicateImage = BitmapFactory.decodeFile(fileUri.getPath());
            FileOutputStream duplicateFile = new FileOutputStream(fileUri2.getPath());
            duplicateImage.compress(Bitmap.CompressFormat.PNG, 100, duplicateFile);
            duplicateFile.close();

            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            InputStream is2 = getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeDir2 = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile2 = new File(cascadeDir2, "eye.xml");
            FileOutputStream os2 = new FileOutputStream(mCascadeFile2);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            byte[] buffer2 = new byte[4096];
            int bytesRead2;
            while ((bytesRead2 = is2.read(buffer2)) != -1) {
                os2.write(buffer2, 0, bytesRead2);
            }
            is2.close();
            os2.close();

            face_cascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            eyes_cascade = new CascadeClassifier(mCascadeFile2.getAbsolutePath());

            if (face_cascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                face_cascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            if (eyes_cascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                eyes_cascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile2.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }

        Mat frame = Highgui.imread(fileUri.getPath());

        Mat frame_gray = new Mat();
        Imgproc.cvtColor(frame, frame_gray, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.equalizeHist(frame_gray, frame_gray);
        //process face detection
        MatOfRect faces = new MatOfRect();

        face_cascade.detectMultiScale(frame_gray, faces, 1.1, 2, 0, new Size(30,30), new Size());

        Rect[] facesArray = faces.toArray();

        nFaces = facesArray.length;

        for(int i=0; i<facesArray.length; i++)
        {
            Core.rectangle(frame, new Point(facesArray[i].x, facesArray[i].y), new Point(facesArray[i].x + facesArray[i].width, facesArray[i].y + facesArray[i].height), new Scalar(0, 255, 0), 5);

            Mat faceROI = frame_gray.submat(facesArray[i]);
            MatOfRect eyes = new MatOfRect();

            //-- In each face, detect eyes
            eyes_cascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0,new Size(30,30), new Size());

            Rect[] eyesArray = eyes.toArray();
            if(eyesArray.length > 0){
                nEyes = eyesArray.length;
            } else {
                nEyes = 1;
            }

            if(eyesArray.length > 0){
                for (int j = 0; j < nEyes; j++)
                {
//                    Point center1 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5, facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
//                    int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
//                    Core.circle(frame, center1, radius, new Scalar(255, 0, 0), 4, 8, 0);

//                    Core.rectangle(frame, new Point(facesArray[i].x + eyesArray[j].x, facesArray[i].y + eyesArray[j].y), new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width, facesArray[i].y + eyesArray[j].y + eyesArray[j].height), new Scalar(0, 255, 0));
                    xMin[i][j] = facesArray[i].x + eyesArray[j].x;
                    yMin[i][j] = facesArray[i].y + eyesArray[j].y;
                    xMax[i][j] = facesArray[i].x + eyesArray[j].x + eyesArray[j].width;
                    yMax[i][j] = facesArray[i].y + eyesArray[j].y + eyesArray[j].height;
                }
            } else {
                for (int j = 0; j < nEyes; j++)
                {
                    xMin[i][j] = (((facesArray[i].x + facesArray[i].width) - facesArray[i].x) / 8) + facesArray[i].x;
                    yMin[i][j] = (((facesArray[i].y + facesArray[i].height) - facesArray[i].y) / 4) + facesArray[i].y;
                    xMax[i][j] = (facesArray[i].x + facesArray[i].width) - (((facesArray[i].x + facesArray[i].width) - facesArray[i].x) / 8);
                    yMax[i][j] = (((facesArray[i].y + facesArray[i].height) - facesArray[i].y) / 2) + facesArray[i].y;
                }
            }

            System.out.println(eyesArray.length + " eyes detected");
        }

        //Highgui.imwrite(fileUri.getPath(), frame);
        Mat new_frame = new Mat();
        Imgproc.cvtColor(frame, new_frame, Imgproc.COLOR_RGB2BGRA);
        Bitmap faceDetected = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(new_frame, faceDetected);

        if(facesArray.length > 0){
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
            final View view = factory.inflate(R.layout.image_holder, null);

            ImageView displayImage = (ImageView) view.findViewById(R.id.faceAlert);
            displayImage.setImageBitmap(faceDetected);

            alertDialogBuilder.setView(view);

            alertDialogBuilder.setMessage("Apply censorship?").setTitle("Face/s detected!");

            alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    Toast.makeText(MainActivity.this, "Processing", Toast.LENGTH_LONG).show();
                    image = null;
                    int n1, n2;
                    int tempPixel = 0;
                    Random rand = new Random();

                    try {
                        image = BitmapFactory.decodeFile(fileUri.getPath());
                        Bitmap imageCopy = image.copy(Bitmap.Config.ARGB_8888, true);
                        if (image != null) {
                            Log.i(TAG, "image is good");
                        }

                        for(int f = 0; f< nFaces; f++) {
                            for (int k = 0; k < nEyes; k++) {
                                for (int i = xMin[f][k]; i < xMax[f][k]; i++) {//i=xMin; i<xMax
                                    for (int j = yMin[f][k]; j < yMax[f][k]; j++) {//j=yMin; j<yMax
                                        n1 = rand.nextInt(xMax[f][k] - xMin[f][k]) + xMin[f][k];//rand.nextInt(xMax-xMin) + xMin
                                        n2 = rand.nextInt(yMax[f][k] - yMin[f][k]) + yMin[f][k];//rand.nextInt(yMax-yMin) + yMin
                                        try{
                                            tempPixel = image.getPixel(i, j);
                                        } catch(NullPointerException s){
                                            s.printStackTrace();
                                        }
                                        //System.out.println("n1 = " + n1 + " n2 = " + n2);
                                        imageCopy.setPixel(i, j, image.getPixel(n1, n2));
                                        imageCopy.setPixel(n1, n2, tempPixel);
                                    }
                                }
                            }
                        }

                        FileOutputStream outputFile = new FileOutputStream(fileUri.getPath());
                        imageCopy.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
                        outputFile.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (requestCode == IMAGE_CAPTURE && resultCode == RESULT_OK) {
                        setPic(imageView, fileUri.getPath());
                        editButton1.setVisibility(View.VISIBLE);
                        editButton2.setVisibility(View.VISIBLE);
                        saveButton.setVisibility(View.VISIBLE);
                    }

                    arg0.dismiss();
                }
            });

            alertDialogBuilder.setNegativeButton("Retake Photo",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //Toast.makeText(MainActivity.this, "You clicked the no button", Toast.LENGTH_LONG).show();
                    File disregard = new File(fileUri.getPath());
                    boolean deleted = disregard.delete();
                    if(deleted){
                        System.out.println("file has been deleted");
                    }else{
                        System.out.println("file has been deleted");
                    }
                    takePicture();

                    dialog.dismiss();
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            if (requestCode == IMAGE_CAPTURE && resultCode == RESULT_OK) {
                setPic(imageView, fileUri.getPath());
                Toast.makeText(MainActivity.this, "No Face Detected!", Toast.LENGTH_LONG).show();
                saveButton.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, Intent data) {
        if (requestCode == IMAGE_CAPTURE && resultCode == RESULT_OK) {
            applyCensor(requestCode, resultCode, data);
        } else if (resultCode == 90){
            setPic(imageView, fileUri.getPath());
        }
    }

    private void takePicture() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFileStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File mediaFile = new File (imageFileStorage.getPath() + File.pathSeparator + "IMG_" + getTimeStamp() + ".png");
        File mediaFileDuplicate = new File (imageFileStorage.getPath() + File.pathSeparator + "IMG_" + getTimeStamp() + " (2).png");
        fileUri = Uri.fromFile(mediaFile);
        fileUri2 = Uri.fromFile(mediaFileDuplicate);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, IMAGE_CAPTURE);

    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setPic(ImageView mImageView, String mCurrentPhotoPath) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();//mImageView.getWidth();
        int targetH = imageView.getHeight();// mImageView.getHeight();
        Log.e("width and height", targetW + targetH + "");

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    public String getTimeStamp () {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public class CensorCrawledImages extends AsyncTask<Void, Void , Void> {
        @Override
        protected Void doInBackground(Void... params) {
            File f = new File("/sdcard/DCIM/Crawled Images");
            File[] list = f.listFiles();
            String outputPath;

            for (int i = 0; i < list.length; i++) {
                //System.out.println(list[i].getAbsolutePath());
                outputPath = "/sdcard/DCIM/Processed Images/" + list[i].getName();
                applyCensor(list[i].getAbsolutePath(), outputPath);
            }

            System.out.println("Processing done!");

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MainActivity.this, "Processing is done!", Toast.LENGTH_SHORT).show();
            viewFiles.setVisibility(View.VISIBLE);
        }
    }

    public void applyCensor(String cImgPath, String cOutPath) {
        CascadeClassifier face_cascade = null;
        CascadeClassifier eyes_cascade = null;
        int nFaces = 0;
        int nEyes = 0;
        int xMin[][] = new int[20][20];
        int yMin[][] = new int[20][20];
        int xMax[][] = new int[20][20];
        int yMax[][] = new int[20][20];
        Bitmap image;

        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            InputStream is2 = getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeDir2 = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile2 = new File(cascadeDir2, "eye.xml");
            FileOutputStream os2 = new FileOutputStream(mCascadeFile2);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            byte[] buffer2 = new byte[4096];
            int bytesRead2;
            while ((bytesRead2 = is2.read(buffer2)) != -1) {
                os2.write(buffer2, 0, bytesRead2);
            }
            is2.close();
            os2.close();

            face_cascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            eyes_cascade = new CascadeClassifier(mCascadeFile2.getAbsolutePath());

            if (face_cascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                face_cascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            if (eyes_cascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                eyes_cascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile2.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }

        Mat frame = Highgui.imread(cImgPath);

        Mat frame_gray = new Mat();
        Imgproc.cvtColor(frame, frame_gray, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.equalizeHist(frame_gray, frame_gray);
        //process face detection
        MatOfRect faces = new MatOfRect();

        face_cascade.detectMultiScale(frame_gray, faces, 1.1, 2, 0, new Size(30, 30), new Size());

        Rect[] facesArray = faces.toArray();

        nFaces = facesArray.length;

        for (int i = 0; i < facesArray.length; i++) {
            Mat faceROI = frame_gray.submat(facesArray[i]);
            MatOfRect eyes = new MatOfRect();

            //-- In each face, detect eyes
            eyes_cascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0, new Size(30, 30), new Size());

            Rect[] eyesArray = eyes.toArray();
            if (eyesArray.length > 0) {
                nEyes = eyesArray.length;
            } else {
                nEyes = 1;
            }

            if (eyesArray.length > 0) {
                for (int j = 0; j < nEyes; j++) {
//                    Point center1 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5, facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
//                    int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
//                    Core.circle(frame, center1, radius, new Scalar(255, 0, 0), 4, 8, 0);

                    //Core.rectangle(frame, new Point(facesArray[i].x + eyesArray[j].x, facesArray[i].y + eyesArray[j].y), new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width, facesArray[i].y + eyesArray[j].y + eyesArray[j].height), new Scalar(0, 255, 0));
                    xMin[i][j] = facesArray[i].x + eyesArray[j].x;
                    yMin[i][j] = facesArray[i].y + eyesArray[j].y;
                    xMax[i][j] = facesArray[i].x + eyesArray[j].x + eyesArray[j].width;
                    yMax[i][j] = facesArray[i].y + eyesArray[j].y + eyesArray[j].height;
                }
            } else {
                for (int j = 0; j < nEyes; j++) {
                    xMin[i][j] = (((facesArray[i].x + facesArray[i].width) - facesArray[i].x) / 8) + facesArray[i].x;
                    yMin[i][j] = (((facesArray[i].y + facesArray[i].height) - facesArray[i].y) / 4) + facesArray[i].y;
                    xMax[i][j] = (facesArray[i].x + facesArray[i].width) - (((facesArray[i].x + facesArray[i].width) - facesArray[i].x) / 8);
                    yMax[i][j] = (((facesArray[i].y + facesArray[i].height) - facesArray[i].y) / 2) + facesArray[i].y;
                }
            }

            System.out.println(eyesArray.length + " eyes detected");
        }

        //Highgui.imwrite(fileUri.getPath(), frame);
        if (facesArray.length > 0) {
            image = null;
            int n1, n2;
            int tempPixel = 0;
            Random rand = new Random();

            try {
                image = BitmapFactory.decodeFile(cImgPath);
                Bitmap imageCopy = image.copy(Bitmap.Config.ARGB_8888, true);
                if (image != null) {
                    Log.i(TAG, "image is good");
                }

                for(int f = 0; f< nFaces; f++) {
                    for (int k = 0; k < nEyes; k++) {
                        for (int i = xMin[f][k]; i < xMax[f][k]; i++) {//i=xMin; i<xMax
                            for (int j = yMin[f][k]; j < yMax[f][k]; j++) {//j=yMin; j<yMax
                                n1 = rand.nextInt(xMax[f][k] - xMin[f][k]) + xMin[f][k];//rand.nextInt(xMax-xMin) + xMin
                                n2 = rand.nextInt(yMax[f][k] - yMin[f][k]) + yMin[f][k];//rand.nextInt(yMax-yMin) + yMin
                                tempPixel = image.getPixel(i, j);
                                //System.out.println("n1 = " + n1 + " n2 = " + n2);
                                imageCopy.setPixel(i, j, image.getPixel(n1, n2));
                                imageCopy.setPixel(n1, n2, tempPixel);
                            }
                        }
                    }
                }

                FileOutputStream outputFile = new FileOutputStream(cOutPath);
                imageCopy.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
                outputFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else{
//            try{
//                FileOutputStream outputFile = new FileOutputStream(cOutPath);
//                imageCopy.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
//                outputFile.close();
//            } catch (IOException e){
//                e.printStackTrace();
//            }

            Highgui.imwrite(cOutPath, frame);
        }
    }
}