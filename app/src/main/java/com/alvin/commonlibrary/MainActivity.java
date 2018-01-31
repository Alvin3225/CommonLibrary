package com.alvin.commonlibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.common.zxing.CaptureActivity;

public class MainActivity extends Activity {

    private TextView tv_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_result = (TextView)findViewById(R.id.tv_result);
    }

    public void sweep(View view) {
        Intent intent = new Intent();
        intent.setClass(this, CaptureActivity.class);
        intent.putExtra("autoEnlarged",true);
        startActivityForResult(intent,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0 && resultCode==RESULT_OK && data!=null){
            String result = data.getStringExtra("result");
            tv_result.setText(result);
        }
    }
}
