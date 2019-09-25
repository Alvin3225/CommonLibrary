/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.common.zxing.decode;

import android.graphics.Bitmap;

import com.common.zxing.CaptureActivity;
import com.common.zxing.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.Map;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;
  private int frameCount;

  DecodeHandler(CaptureActivity activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (message == null || !running) {
      return;
    }
    if (message.what == R.id.decode) {
      decode((byte[]) message.obj, message.arg1, message.arg2);

    } else if (message.what == R.id.quit) {
      running = false;
      Looper.myLooper().quit();

    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    frameCount++;
    //丢弃前2帧并每隔2帧分析下预览帧color值
    if (frameCount > 2 && frameCount % 2 == 0) {
      analysisColor(data, width, height);
    }
    long start = System.currentTimeMillis();
    Result rawResult = null;
    PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
    if (source != null) {
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      try {
        rawResult = multiFormatReader.decodeWithState(bitmap);
      } catch (ReaderException re) {
        // continue
      } finally {
        multiFormatReader.reset();
      }
    }

    Handler handler = activity.getHandler();
    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
        bundleThumbnail(source, bundle);        
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_failed);
        message.sendToTarget();
      }
    }
  }

  private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
    ByteArrayOutputStream out = new ByteArrayOutputStream();    
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
  }

  //分析预览帧中图片的arg 取平均值
  private void analysisColor(byte[] data, int width, int height) {
    int[] rgb = decodeYUV420SP(data, width / 8, height / 8);
    Bitmap bmp = Bitmap.createBitmap(rgb, width / 8, height / 8, Bitmap.Config.ARGB_8888);
    if (bmp != null) {
      //取以中心点宽高10像素的图片来分析
      Bitmap resizeBitmap = Bitmap.createBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, 10, 10);
      float color = (float) getAverageColor(resizeBitmap);
      DecimalFormat decimalFormat1 = new DecimalFormat("0.00");
      String percent = decimalFormat1.format(color / -16777216);
      float floatPercent = Float.parseFloat(percent);
      Handler handler = activity.getHandler();
      if(floatPercent >= 0.99 && floatPercent <= 1.00){//弱光
        Message message = Message.obtain(handler, R.id.show_flash_light, 1);
          handler.sendMessage(message);
      }else{
        Message message = Message.obtain(handler, R.id.show_flash_light, 2);
          handler.sendMessage(message);
      }
      resizeBitmap.recycle();
      bmp.recycle();
    }
  }

  private int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
    final int frameSize = width * height;

    int rgb[] = new int[width * height];
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
      for (int i = 0; i < width; i++, yp++) {
        int y = (0xff & ((int) yuv420sp[yp])) - 16;
        if (y < 0) y = 0;
        if ((i & 1) == 0) {
          v = (0xff & yuv420sp[uvp++]) - 128;
          u = (0xff & yuv420sp[uvp++]) - 128;
        }

        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        if (r < 0) r = 0;
        else if (r > 262143) r = 262143;
        if (g < 0) g = 0;
        else if (g > 262143) g = 262143;
        if (b < 0) b = 0;
        else if (b > 262143) b = 262143;

        rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                0xff00) | ((b >> 10) & 0xff);


      }
    }
    return rgb;
  }

  private int getAverageColor(Bitmap bitmap) {
    int redBucket = 0;
    int greenBucket = 0;
    int blueBucket = 0;
    int pixelCount = 0;

    for (int y = 0; y < bitmap.getHeight(); y++) {
      for (int x = 0; x < bitmap.getWidth(); x++) {
        int c = bitmap.getPixel(x, y);

        pixelCount++;
        redBucket += Color.red(c);
        greenBucket += Color.green(c);
        blueBucket += Color.blue(c);
      }
    }

    int averageColor = Color.rgb(redBucket / pixelCount, greenBucket
            / pixelCount, blueBucket / pixelCount);
    return averageColor;
  }

}
