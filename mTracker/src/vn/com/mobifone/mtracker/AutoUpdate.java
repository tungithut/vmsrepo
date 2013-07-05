package vn.com.mobifone.mtracker;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
 
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.TextView;
 
public class AutoUpdate extends Activity {
 
    TextView tv_loading;
    int downloadedSize = 0, totalsize;
    float per = 0;
     
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Dynamically layout:
        tv_loading = new TextView(this);
        setContentView(tv_loading);
        tv_loading.setGravity(Gravity.CENTER);
        tv_loading.setTypeface(null, Typeface.BOLD);
        
        // Download apk and auto installation
        downloadAndOpenAPK();
    }
 
    void downloadAndOpenAPK() {
        new Thread(new Runnable() {
            public void run() {
                Uri path = Uri.fromFile(downloadFile(VMSConstants.APK_URL));
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(path, "application/vnd.android.package-archive");
                    
                    //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //intent.setFlags(Intent.ACTION_PACKAGE_REPLACED);
                    
                    startActivity(intent);
                    
                    finish();
                } catch (ActivityNotFoundException e) {
                    tv_loading
                            .setError("Error while install new version");
                }
            }
        }).start();
   
    }
 
    File downloadFile(String dwnload_file_path) {
        File file = null;
        try {
 
            URL url = new URL(dwnload_file_path);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("GET");
            
            // connect
            urlConnection.connect();
            
            // set the path where we want to save the file
            File SDCardRoot = Environment.getExternalStorageDirectory();
            String PATH = SDCardRoot + VMSConstants.APP_HOME;
            
            File fileDir = new File(PATH);
            if (!fileDir.exists()){
            	fileDir.mkdirs();
            }
            
            // create a new file, to save the downloaded file
            file = new File(fileDir, "mtracker.apk");
 
            FileOutputStream fileOutput = new FileOutputStream(file);
                        
            // Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();
 
            // this is the total size of the file which we are
            // downloading
            totalsize = urlConnection.getContentLength();
            setText("Starting APK download...");
 
            // create a buffer...
            byte[] buffer = new byte[1024 * 1024];  
            int bufferLength = 0;
 
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                per = ((float) downloadedSize / totalsize) * 100;
                setText("Total APK File size  : "
                        + (totalsize / 1024)
                        + " KB\n\nDownloading APK " + (int) per
                        + "% complete");
            }
            // close the output stream when complete //
            fileOutput.close();
            setText("Download Complete. Open Installer to install the Application.");
 
        } catch (final MalformedURLException e) {
            setTextError("Some error occured. Press back and try again.",
                    Color.RED);
        } catch (final IOException e) {
            setTextError("Some error occured. Press back and try again.",
                    Color.RED);
        } catch (final Exception e) {
            setTextError(
                    "Failed to download image. Please check your internet connection.",
                    Color.RED);
        }
        return file;
    }
 
    void setTextError(final String message, final int color) {
        runOnUiThread(new Runnable() {
            public void run() {
                tv_loading.setTextColor(color);
                tv_loading.setText(message);
            }
        });
 
    }
 
    void setText(final String txt) {
        runOnUiThread(new Runnable() {
            public void run() {
                tv_loading.setText(txt);
            }
        });
 
    }
 
}