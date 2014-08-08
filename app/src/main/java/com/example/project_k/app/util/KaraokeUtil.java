package com.example.project_k.app.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by wchau on 8/7/14.
 */
public class KaraokeUtil {
    public static void copyFile(String assetPath, String localPath, Context context) {
        try {
            InputStream in = context.getAssets().open(assetPath);
            FileOutputStream out = new FileOutputStream(localPath);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
            Process nativeApp = Runtime.getRuntime().exec(
                "chmod 777 " + localPath
            );
            nativeApp.waitFor();
        } catch (IOException e) {
            Log.e("WTF", e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Log.e("WTF", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void combine(String filename1, String filename2) {
        try {
            String[] envp = {"LD_LIBRARY_PATH=/data/data/com.example.project_k.app/lib:$LD_LIBRARY_PATH"};
            Process nativeApp = Runtime.getRuntime().exec(
                "/data/data/com.example.project_k.app/files/ffmpeg -i " + filename1 + " -i " + filename2
                + " -c:v copy -c:a aac -filter_complex [0:a][1:a]amerge[aout] -map 0:v:0 -map [aout] -ac 1 -b:a 32k "
                + "-strict experimental /sdcard/Download/output.3gp -y", envp);
            //Process nativeApp = Runtime.getRuntime().exec(
            //        "/data/data/com.example.project_k.app/files/ffmpeg -i " + filename1 + " -i " + filename2
            //                + " -filter_complex [0:a][1:a]amerge[aout] -map [aout] /data/data/com.example.project_k.app/files/output.wav -y", envp);

            BufferedReader reader = new BufferedReader(new InputStreamReader(nativeApp.getErrorStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            nativeApp.waitFor();

            String nativeOutput =  output.toString();
            Log.e("WHAT?", nativeOutput);
        } catch (RuntimeException e) {
            Log.e("WTF", "RuntimeException");
            return;
        } catch (IOException e) {
            Log.e("WTF", e.getMessage());
            return;
        } catch (InterruptedException e) {
            Log.e("WTF", "InterruptedException");
            return;
        }
    }
}