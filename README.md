# CommonLibrary
common libraries
使用说明：

**这个库有一段时间没有优化更新了，有些是可以优化的，比如放大级别和锁屏息屏重新打开黑屏问题，这些后期再优化吧，大家也可以把源码clone下来自己修改优化的。另外，放大摄像头问题，更新到最新的zxing库之后，这个放大就有点鸡肋了，感觉用不到，放大的地方建议可以采用计时操作，进入扫描界面几秒后没有任何结果，可以放大一次，并调用一下重置，这样要比之前好很多**


因为改library在较新的编译环境下进行，所以如果引入有问题按照错误提示进行配置环境

添加依赖
```Java
compile 'com.github.Alvin9234:CommonLibrary:1.0.7'

项目根目录的gradle添加
allprojects {
    repositories {
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}
```

2、使用方法

autoEnlarged ，二维码扫描是否要自动放大摄像头，默认不放大
```Java
Intent intent = new Intent();
intent.setClass(this, CaptureActivity.class);
intent.putExtra("autoEnlarged",false);
startActivityForResult(intent,0);
```

在onActivityResult回调扫描结果
```Java
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0 && resultCode==RESULT_OK && data!=null){
            String result = data.getStringExtra("result");
	//TODO
            ToastUtil.show(this,result);
        }
    }

```
有关二维码放大的问题，大家可以移步我的博客，有说明。http://blog.csdn.net/u010705554/article/details/78204090
或者 https://www.jianshu.com/p/710e3d29dfaf
