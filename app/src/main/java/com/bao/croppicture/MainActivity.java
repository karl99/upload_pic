package com.bao.croppicture;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //  private PermissionUtils permissionUtils;
    private CropImageUtils cropImageUtils;

    private static final String[] PERMISSIONS = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ArrayList<String> deniedPermission = new ArrayList<>();
    private static final int PERMISSION_CODE = 1000;

    String imagepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_photo).setOnClickListener(this);
        findViewById(R.id.btn_album).setOnClickListener(this);
        findViewById(R.id.btn_upload).setOnClickListener(this);
        //初始化裁剪工具
        cropImageUtils = new CropImageUtils(this);


        //申请权限

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            deniedPermission.clear();
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int result = grantResults[i];
                if (result != PackageManager.PERMISSION_GRANTED) {
                    deniedPermission.add(permission);
                }
            }

            if (deniedPermission.isEmpty()) {
                //Toast.makeText(MainActivity.this, "所有权限已被授予。", Toast.LENGTH_SHORT).show();
                ToastUtils.INSTANCE.longToast("所有权限已被授予.");
                //               startCamera();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.capture_permission_message))
                        .setNegativeButton(getString(R.string.capture_permission_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                MainActivity.this.finish();
                            }
                        })
                        .setPositiveButton(getString(R.string.capture_permission_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] denied = new String[deniedPermission.size()];
                                ActivityCompat.requestPermissions(MainActivity.this, deniedPermission.toArray(denied), PERMISSION_CODE);
                            }
                        }).create().show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //拍照
            case R.id.btn_photo:
                cropImageUtils.takePhoto();
                break;
            //打开相册
            case R.id.btn_album:
                cropImageUtils.openAlbum();
                break;
            case R.id.btn_upload:
                ToastUtils.INSTANCE.longToast("上传：" + imagepath);
              //
                uploadFile(imagepath);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cropImageUtils.onActivityResult(requestCode, resultCode, data, new CropImageUtils.OnResultListener() {
            @Override
            public void takePhotoFinish(String path) {
                ToastUtils.INSTANCE.longToast("照片存放在：" + path);
                imagepath=path;
                //拍照回调，去裁剪
                cropImageUtils.cropPicture(path);
            }

            @Override
            public void selectPictureFinish(String path) {
                ToastUtils.INSTANCE.longToast("打开图片：" + path);
                imagepath=path;
                //相册回调，去裁剪

                cropImageUtils.cropPicture(path);
            }

            @Override
            public void cropPictureFinish(String path) {
                ToastUtils.INSTANCE.longToast("裁剪保存在：" + path);
                imagepath=path;
                //裁剪回调
                Glide.with(MainActivity.this)
                        .load(path)
                        .into((ImageView) findViewById(R.id.image_result));
            }
        });
    }
    public  String getFileName(String pathandname){
        int  start=pathandname.lastIndexOf( "/" );
        int  end=pathandname.lastIndexOf( "." );
        if  (start!=- 1  && end!=- 1 ) {
            return  pathandname.substring(start+ 1 , pathandname.length());
        }
        else  {
            return  null ;
        }
    }
    // 使用OkHttp上传文件
    public void uploadFile(String imagepath) {

        File file = new File(imagepath);

        OkHttpClient client = new OkHttpClient();
      //  MediaType contentType = MediaType.parse("text/plain"); // 上传文件的Content-Type
      //  MediaType contentType = MediaType.parse("multipart/form-data"); // 上传文件的Content-Type

      //  RequestBody body = RequestBody.create(contentType, file); // 上传文件的请求体
        String filename= getFileName(imagepath);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename,
                        RequestBody.create(MediaType.parse("multipart/form-data"), file))
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.3.27:8080/api/upload.ashx") // 上传地址
                //.url("https://api.github.com/markdown/raw") // 上传地址
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 文件上传成功
                if (response.isSuccessful()) {
                    Log.i("Haoxueren", "onResponse: " + response.body().string());
                } else {
                    Log.i("Haoxueren", "onResponse: " + response.message());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 文件上传失败
                Log.i("Haoxueren", "onFailure: " + e.getMessage());
            }
        });
    }
}
