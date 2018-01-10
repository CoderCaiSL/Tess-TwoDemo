package com.cv4j.ocrdemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cv4j.ocrdemo.app.BaseActivity;
import com.cv4j.ocrdemo.camera.EasyCamera;
import com.cv4j.ocrdemo.camera.util.DisplayUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.cv4j.ocrdemo.R.id.result;
import static com.cv4j.ocrdemo.camera.EasyCamera.REQUEST_CAPTURE;

/**
 * Created by csl on 2018/1/9.
 */

public class TestChineseActivity extends BaseActivity{
    private static final String TAG = TestEnglishActivity.class.getSimpleName();
    private Button btnCapture;
    private ImageView ivImage;
    private ImageView ivImage2;
    private TextView resultView;
    private int screenWidth;
    private float ratio = 0.5f; //取景框高宽比

    private TessBaseAPI tessBaseApi;
    private static final String lang = "chi_sim";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/";
    private static final String TESSDATA = "tessdata";
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_chinese);

        ivImage = (ImageView) findViewById(R.id.iv_image);
        ivImage2 = (ImageView) findViewById(R.id.iv_image2);
        btnCapture = (Button) findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String SAMPLE_CROPPED_IMAGE_NAME = "cropImage_"+System.currentTimeMillis()+".png";
                Uri destination = Uri.fromFile(new File(getCacheDir(), SAMPLE_CROPPED_IMAGE_NAME));
                EasyCamera.create(destination)
                        .withViewRatio(ratio)
                        .withMarginCameraEdge(50,50)
                        .start(TestChineseActivity.this);
            }
        });
        //原始照相
        findViewById(R.id.btn_capture2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file=new File(Environment.getExternalStorageDirectory(), "/temp/"+System.currentTimeMillis() + ".jpg");
                if (!file.getParentFile().exists())file.getParentFile().mkdirs();
                imageUri = Uri.fromFile(file);
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//设置Action为拍照
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//将拍取的照片保存到指定URI
                startActivityForResult(intent,10001);
            }
        });
        //相册
        findViewById(R.id.btn_capture3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //就是利用Intent调用系统的相册
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");//相片类型
                startActivityForResult(intent,10002);
            }
        });
        screenWidth = (int) DisplayUtils.getScreenWidth(this);
        ivImage.setLayoutParams(new LinearLayout.LayoutParams(screenWidth, (int) (screenWidth * ratio)));
        ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        resultView = (TextView) findViewById(result);
        prepareTesseract();

    }
    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyTessDataFiles(TESSDATA);
    }
    /**
     * Prepare directory on external storage
     *
     * @param path
     * @throws Exception
     */
    private void prepareDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }
    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     * @param path - name of directory with .traineddata files
     */
    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {
                    InputStream in = getAssets().open(path + "/" + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*if (resultCode == RESULT_OK) {
            if (requestCode == EasyCamera.REQUEST_CAPTURE) {
                Uri resultUri = EasyCamera.getOutput(data);
                int width = EasyCamera.getImageWidth(data);
                int height = EasyCamera.getImageHeight(data);
                ivImage.setImageURI(resultUri);
                Log.i(TAG,"imageWidth:"+width);
                Log.i(TAG,"imageHeight:"+height);
                startOCR(resultUri);
            }
        }*/
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case REQUEST_CAPTURE :
                    Uri resultUri = EasyCamera.getOutput(data);
                    int width = EasyCamera.getImageWidth(data);
                    int height = EasyCamera.getImageHeight(data);
                    ivImage.setImageURI(resultUri);
                    Log.i(TAG,"imageWidth:"+width);
                    Log.i(TAG,"imageHeight:"+height);
                    startOCR(resultUri);
                    break;
                case 10001:
                    Uri uri1 ;
                    if (data == null){
                        uri1 = imageUri;
                    }else {
                        uri1 = data.getData();
                    }
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ivImage2.setImageBitmap(bitmap);
                    String result = extractText(bitmap);
                    resultView.setText(result);
                    break;
                case 10002:
                    Uri uri2 = data.getData();
                    Bitmap bitmap2 = null;
                    try {
                        bitmap2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ivImage2.setImageBitmap(bitmap2);
                    String result2 = extractText(bitmap2);
                    resultView.setText(result2);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * don't run this code in main thread - it stops UI thread. Create AsyncTask instead.
     * http://developer.android.com/intl/ru/reference/android/os/AsyncTask.html
     *
     * @param imgUri
     */
    private void startOCR(Uri imgUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            Bitmap bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);
            String result = extractText(bitmap);
            resultView.setText(result);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private String extractText(final Bitmap bitmap) {
        final String[] extractedText = {"empty result"};
        final ProgressDialog dialog = ProgressDialog.show(this,"解析","我解我解我解解解...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tessBaseApi = new TessBaseAPI();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    if (tessBaseApi == null) {
                        Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
                    }
                }
                tessBaseApi.init(DATA_PATH, lang);
                tessBaseApi.setImage(bitmap);
                extractedText[0] = tessBaseApi.getUTF8Text();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultView.setText(extractedText[0]);
                        dialog.dismiss();
                    }
                });
                tessBaseApi.end();
            }
        }).start();
        return extractedText[0];
    }
}
