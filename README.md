# CommonLibrary
common libraries
使用说明：

因为改library在较新的编译环境下进行，所以如果引入有问题按照错误提示进行配置环境

添加依赖
compile 'com.github.Alvin9234:CommonLibrary:1.0.6'

项目根目录的gradle添加
allprojects {
    repositories {
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}


2、使用方法

autoEnlarged ，二维码扫描是否要自动放大摄像头，默认不放大

Intent intent = new Intent();
intent.setClass(this, CaptureActivity.class);
intent.putExtra("autoEnlarged",false);
startActivityForResult(intent,0);


在onActivityResult回调扫描结果
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0 && resultCode==RESULT_OK && data!=null){
            String result = data.getStringExtra("result");
	//TODO
            ToastUtil.show(this,result);
        }
    }

有关二维码放大的问题，大家可以移步我的博客，有说明。http://blog.csdn.net/u010705554/article/details/78204090
