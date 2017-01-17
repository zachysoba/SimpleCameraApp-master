package com.dlsu.getbetter.cameraapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.RunnableFuture;
import java.util.logging.LogRecord;

public class ImageEdit extends AppCompatActivity {

    ImageView imageView;
    TextView textView;
    int x;
    int y;
    int topX;
    int topY;
    int bottomX;
    int bottomY;
    int flag = 0;
    int undoFlag = 1;
    String getImage;
    String getImage2;
    Button addButton;
    Button undoButton;
    Button doneButton;
    Bitmap imageCopy;
    Bitmap imageUndo;
    Bitmap drawableImage;
    Bitmap undoDrawable = null;

    AlertDialog c;

    Paint paint;
    Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        Intent receive = getIntent();
        getImage = (String) receive.getStringExtra("image");
        getImage2 = (String) receive.getStringExtra("image2");

        imageView = (ImageView) findViewById(R.id.image1);
        textView = (TextView) findViewById(R.id.text1);

        addButton = (Button) findViewById(R.id.button1);
        undoButton = (Button) findViewById(R.id.button2);
        doneButton = (Button) findViewById(R.id.button3);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        BitmapFactory.decodeFile(getImage2, bmOptions);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(getImage2, bmOptions);
        imageView.setImageBitmap(bitmap);

        setTitle("GetBetter");

        drawableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
//        canvas = new Canvas(drawableImage);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoFlag++;
                //canvas = new Canvas(drawableImage);
                alertBound();
            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoButton.setVisibility(View.GONE);
                undoFlag--;
                imageView.setImageBitmap(imageUndo);
                imageCopy = imageUndo;
                undoDrawable = imageCopy;
//                try {
//                    FileOutputStream outputFile = new FileOutputStream(getImage);
//                    imageUndo.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
//                    outputFile.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                drawableImage = imageCopy;
                drawableImage = undoDrawable.copy(Bitmap.Config.ARGB_8888, true);
                if(undoFlag == 0){
                    System.out.print("it goes in here");
                    undoButton.setVisibility(View.GONE);
                    doneButton.setVisibility(View.GONE);
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FileOutputStream outputFile = new FileOutputStream(getImage);
                    imageCopy.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
                    outputFile.close();
                } catch(IOException e){
                    e.printStackTrace();
                }
                setResult(90);
                finish();
            }
        });

        alertBound();
    }

    public boolean onTouchEvent(MotionEvent event) {

        x = (int)event.getX();
        y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
        }

        //System.out.println("edit touch x= " + x + " edit touch y= "+ y);
        return false;
    }

    public void onWindowFocusChanged (boolean hasFocus) {
        //to get imageView coordinates
        int[]imageCoords = new int[2];
        imageView.getLocationOnScreen(imageCoords);

        System.out.println("imageEdit x=" + imageCoords[0] + " imageEdit y=" + imageCoords[1]);
    }

    public void alertBound(){
        undoButton.setVisibility(View.GONE);

        textView.setText("Tap to set upper left corner of censor region");

        canvas = new Canvas(drawableImage);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                topX = (int)event.getX();
                topY = (int)event.getY();
                System.out.println("top x = " + topX + " top y = " + topY);
                canvas.drawCircle(topX, topY, 10, paint);
                imageView.setAdjustViewBounds(true);
                imageView.setImageBitmap(drawableImage);
                alertBound2();
                return false;
            }
        });
    }

    public void handler(int time) {
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                placeCensor();
            }
        }, time);
    }

    public void alertBound2(){

        textView.setText("Tap to set lower right corner of censor region");

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                bottomX = (int)event.getX();
                bottomY = (int)event.getY();
                System.out.println("bottom x = " + bottomX + " bottom y = " + bottomY);
                canvas.drawCircle(bottomX, bottomY, 10, paint);
                imageView.setAdjustViewBounds(true);
                imageView.setImageBitmap(drawableImage);

                alertBox();

                handler(100);

                addButton.setVisibility(View.VISIBLE);
                undoButton.setVisibility(View.VISIBLE);
                doneButton.setVisibility(View.VISIBLE);

                return false;
            }
        });

    }

    public void placeCensor(){

        Bitmap image = null;
        int n1, n2;
        int tempPixel = 0;
        Random rand = new Random();

//        try {
            if(flag == 0){
                image = BitmapFactory.decodeFile(getImage2);
            } else {
                image = imageCopy;
            }

            imageCopy = image.copy(Bitmap.Config.ARGB_8888, true);
            imageUndo = image.copy(Bitmap.Config.ARGB_8888, true);

            for (int i = topX; i < bottomX; i++) {//i=xMin; i<xMax
                for (int j = topY; j < bottomY; j++) {//j=yMin; j<yMax
                    n1 = rand.nextInt(bottomX - topX) + topX;//rand.nextInt(xMax-xMin) + xMin
                    n2 = rand.nextInt(bottomY - topY) + topY;//rand.nextInt(yMax-yMin) + yMin
                    tempPixel = image.getPixel(i, j);
                    imageCopy.setPixel(i, j, image.getPixel(n1, n2));
                    imageCopy.setPixel(n1, n2, tempPixel);
                }
            }

//            FileOutputStream outputFile = new FileOutputStream(getImage);
//            imageCopy.compress(Bitmap.CompressFormat.PNG, 100, outputFile);
//            outputFile.close();

            flag = 1;

            imageView.setOnTouchListener(null);
            textView.setText(null);

            imageView.setImageBitmap(imageCopy);
            image = imageCopy;
            drawableImage = image.copy(Bitmap.Config.ARGB_8888, true);

//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        c.dismiss();
    }

    public void alertBox(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Processing..");

        // Create the AlertDialog object and return it
        c = builder.create();
        c.show();
    }

}
