package com.allen.systemcamerademo;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.img_photo)
    ImageView imgPhoto;
    @BindView(R.id.system_camera)
    Button systemCamera;
    @BindView(R.id.custom_camera)
    Button customCamera;
    private static final int REQUEST_CODE_TAKE_PICTURE_NO_URI = 0;
    private static final int REQUEST_CODE_TAKE_PICTURE_URI = 1;
    private static final int REQUEST_CODE_TAKE_PICTURE_CUSTOM_CAMERA = 2;
    private Uri photoUri;
    private Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getSupportActionBar().hide();
    }


    @OnClick({R.id.img_photo, R.id.system_camera, R.id.custom_camera})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_photo://照片
                break;
            case R.id.system_camera://系统相机
                showWaySelectDialog();
                break;
            case R.id.custom_camera://自定义相机
                Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
                String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/camera_" + System.currentTimeMillis() + ".jpg";
                intent.putExtra("path", path);
                startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE_CUSTOM_CAMERA);
                break;
            default:
                break;

        }
    }


    /**
     * 显示选择系统相机方式的对话框
     */
    private void showWaySelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择方式");
        builder.setMessage("选择调用系统相机的方式，方式一不指定Uri,方式二指定Uri");
        builder.setNegativeButton("方式一", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //系统常量， 启动相机的关键
                startActivityForResult(openCameraIntent, REQUEST_CODE_TAKE_PICTURE_NO_URI); // 参数常量为自定义的request code, 在取返回结果时有用
            }
        });
        builder.setPositiveButton("方式二", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoUri = Uri.fromFile(getSavePhotoFile());
                openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(openCameraIntent, REQUEST_CODE_TAKE_PICTURE_URI);
            }
        });
        AlertDialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    /**
     * 获取保存拍照后图片保存的File
     *
     * @return
     */
    private File getSavePhotoFile() {
        String photoPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/camera_" + System.currentTimeMillis() + ".jpg";
        File file = new File(photoPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {//判断结果是否成功
            return;
        }

        ContentResolver resolver = getContentResolver();
        switch (requestCode) {
            case REQUEST_CODE_TAKE_PICTURE_NO_URI://不指定Uri方式的系统相机回调
                bitmap = data.getParcelableExtra("data");
                imgPhoto.setImageBitmap(bitmap);
                break;
            case REQUEST_CODE_TAKE_PICTURE_URI://指定Uri方式的系统相机回调

                try {
                    bitmap = BitmapFactory.decodeStream(resolver.openInputStream(photoUri));
                    imgPhoto.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case REQUEST_CODE_TAKE_PICTURE_CUSTOM_CAMERA:
                String path = data.getDataString();
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                imgPhoto.setImageBitmap(bitmap);
                break;
            default:
                break;
        }
    }
}

