package com.alvin.commonlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.common.zxing.CaptureActivity;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends Activity {

    private TextView tv_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_result = (TextView)findViewById(R.id.tv_result);
    }

    public void sweep(View view) {
        if(EasyPermissions.hasPermissions(this,Manifest.permission.CAMERA)){
            Intent intent = new Intent();
            intent.setClass(this, CaptureActivity.class);
            intent.putExtra("autoEnlarged",false);
            startActivityForResult(intent,0);
        }else{
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this,1,Manifest.permission.CAMERA).build());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0 && resultCode==RESULT_OK && data!=null){
            String result = data.getStringExtra("result");
            tv_result.setText(result);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
