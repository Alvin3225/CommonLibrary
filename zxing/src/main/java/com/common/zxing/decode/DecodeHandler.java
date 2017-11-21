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
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.common.zxing.R;
import com.common.zxing.CaptureActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

final class DecodeHandler extends Handler {

	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final CaptureActivity activity;

	private final MultiFormatReader multiFormatReader;

	private boolean running = true;
	private boolean autoEnlarged = false;//是否具有自动放大功能(功能仅仅限扫描的是二维码，条形码不放大)

	DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints,boolean autoEnlarged) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
		this.autoEnlarged = autoEnlarged;
	}

	@Override
	public void handleMessage(Message message) {
		if (!running) {
			return;
		}
		int what = message.what;
		if (what == R.id.decode) {
			try {
				decode((byte[]) message.obj, message.arg1, message.arg2);
			} catch (Exception e) {
				// TODO: handle exception
			}

		} else if (what == R.id.quit) {
			running = false;
			Looper.myLooper().quit();

		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		Result rawResult = null;

		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width;
		width = height;
		height = tmp;

		PlanarYUVLuminanceSource source = activity.getCameraManager()
				.buildLuminanceSource(rotatedData, width, height);
		if (source != null) {
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				// 预览界面最终取到的是个bitmap，然后对其进行解码
				rawResult = multiFormatReader.decodeWithState(bitmap);
			}
			catch (ReaderException re) {
				// continue
			}
			finally {
				multiFormatReader.reset();
			}
		}

		Handler handler = activity.getHandler();
		if (rawResult != null) {
			// Don't log the barcode contents for security.
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found barcode in " + (end - start) + " ms");
			if (handler != null) {
				if (rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE && autoEnlarged) {
					Log.e("ssssss", "是二维码");
					//计算扫描框中的二维码的宽度，两点间距离公式
					float point1X = rawResult.getResultPoints()[0].getX();
					float point1Y = rawResult.getResultPoints()[0].getY();
					float point2X = rawResult.getResultPoints()[1].getX();
					float point2Y = rawResult.getResultPoints()[1].getY();
					int len =(int) Math.sqrt(Math.abs(point1X-point2X)*Math.abs(point1X-point2X)+Math.abs(point1Y-point2Y)*Math.abs(point1Y-point2Y));
					Rect frameRect = activity.getCameraManager().getFramingRect();
					if(frameRect!=null){
						int frameWidth = frameRect.right-frameRect.left;
						Camera camera = activity.getCameraManager().getCamera();
						Camera.Parameters parameters = camera.getParameters();
						int maxZoom = parameters.getMaxZoom();
						int zoom = parameters.getZoom();
						if(parameters.isZoomSupported()){//支持放大镜头
							if(len <= frameWidth/4){//二维码在扫描框中的宽度小于扫描框的1/4，放大镜头
								if(zoom==0){
									zoom = maxZoom/2;
								}else if(zoom <= maxZoom-10){
									zoom = zoom+10;
								}else{
									zoom = maxZoom;
								}
								parameters.setZoom(zoom);
								camera.setParameters(parameters);
								//重新扫描
								Message message = Message.obtain(handler, R.id.decode_failed);
								message.sendToTarget();
							}else{
								Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
								Bundle bundle = new Bundle();
								bundleThumbnail(source, bundle);
								message.setData(bundle);
								message.sendToTarget();
							}
						}else{
							Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
							Bundle bundle = new Bundle();
							bundleThumbnail(source, bundle);
							message.setData(bundle);
							message.sendToTarget();
						}
					}else{
						Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
						Bundle bundle = new Bundle();
						bundleThumbnail(source, bundle);
						message.setData(bundle);
						message.sendToTarget();
					}
				}else{
					Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
					Bundle bundle = new Bundle();
					bundleThumbnail(source, bundle);
					message.setData(bundle);
					message.sendToTarget();
				}
			}
		}
		else {
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
		Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
		bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
		bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
	}

}
