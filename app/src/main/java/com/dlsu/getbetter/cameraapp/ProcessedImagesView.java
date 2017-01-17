package com.dlsu.getbetter.cameraapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Created by Raduban on 8/29/2016.
 */
public class ProcessedImagesView extends AppCompatActivity {

    GridView gv, gv2;
    ArrayList<File> list, list2;
    ArrayList<Bitmap> scaledBitmapList, scaledBitmapList2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processed_images_view);

        setTitle("GetBetter");

        scaledBitmapList = new ArrayList<>();
        scaledBitmapList2 = new ArrayList<>();
        list = imageReader(new File ("/sdcard/DCIM/Crawled Images"));
        list2 = imageReader(new File("/sdcard/DCIM/Processed Images"));

        ScaleImages s = new ScaleImages();
        s.execute();

        gv = (GridView) findViewById(R.id.gridView);
        gv2 = (GridView) findViewById(R.id.gridView2);
    }

    class GridAdapter extends BaseAdapter{

        ArrayList<Bitmap> scaledList;

        GridAdapter(ArrayList<Bitmap> list) {
            scaledList = list;
        }
        @Override
        public int getCount(){
            return list.size();
        }

        @Override
        public Object getItem(int position){
            return list.get(position);
        }

        @Override
        public long getItemId(int position){
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            convertView = getLayoutInflater().inflate(R.layout.single_grid, parent, false);
            ImageView iv = (ImageView) convertView.findViewById(R.id.imageView);


            //iv.setImageURI(Uri.parse(getItem(position).toString()));

            iv.setImageBitmap(scaledList.get(position));

            return convertView;
        }
    }

    ArrayList<File> imageReader(File root){
        ArrayList<File> a = new ArrayList<>();

        File[] files = root.listFiles();
        for(int i = 0; i < files.length; i++){
            if(files[i].isDirectory()){
                a.addAll(imageReader(files[i]));
            }
            else {
                if(files[i].getName().endsWith(".jpg")){
                    a.add(files[i]);
                }
            }
        }

        return a;
    }

    public class ScaleImages extends AsyncTask<Void, Void , Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int targetW = 100;
            int targetH = 100;
            // Get the dimensions of the bitmap

            for(int i = 0; i < list.size(); i++){
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                Uri path = Uri.parse(list.get(i).toString());
                System.out.println(list);
                BitmapFactory.decodeFile(path.toString(), bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                Bitmap bitmap = BitmapFactory.decodeFile(path.toString(), bmOptions);
                scaledBitmapList.add(bitmap);
            }


            return null;
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            gv.setAdapter(new GridAdapter(scaledBitmapList));

            ScaleImages2 s2 = new ScaleImages2();
            s2.execute();
        }
    }

    public class ScaleImages2 extends AsyncTask<Void, Void , Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int targetW = 100;
            int targetH = 100;
            // Get the dimensions of the bitmap

            for (int i = 0; i < list2.size(); i++) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                Uri path = Uri.parse(list2.get(i).toString());
                System.out.println(list2);
                BitmapFactory.decodeFile(path.toString(), bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                Bitmap bitmap = BitmapFactory.decodeFile(path.toString(), bmOptions);
                scaledBitmapList2.add(bitmap);
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            gv2.setAdapter(new GridAdapter(scaledBitmapList2));
        }
    }
}
