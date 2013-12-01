package com.sun.parallaxviewpage.imageloader.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.graphics.Bitmap;
import android.util.Log;

import com.sun.imageloader.cache.api.MemoryCache;
import com.sun.imageloader.cache.impl.DiskCache;
import com.sun.imageloader.core.ImageKey;
import com.sun.imageloader.core.ImageSettings;
import com.sun.imageloader.core.ImageWriter;
import com.sun.imageloader.core.api.FailedTaskReason;
import com.sun.imageloader.core.api.FailedTaskReason.ExceptionType;
import com.sun.imageloader.core.api.ImageFailListenter;
import com.sun.imageloader.core.api.ImageTaskListener;
import com.sun.imageloader.downloader.api.ImageRetriever;
import com.sun.imageloader.imagedecoder.api.ImageDecoder;
import com.sun.imageloader.imagedecoder.utils.L;
import com.sun.parallaxviewpage.onimageload.api.OnImageLoad;

public class ImageLoaderTask implements Runnable{

	
	private static final String TAG = ImageLoaderTask.class.getName();
	private static final String THREAD_NAME = null;
	private ImageDecoder _imageDecoder;
	private ImageRetriever _imageRetreiver;
	private ImageWriter _imageWriter;
	private ImageSettings _imageSettings;
	private MemoryCache<ImageKey, File> _diskCache;
	private String _imageDirectoryLocation;
	private OnImageLoad _onImageLoadCallBack;
	private ImageFailListenter _taskListener; 
	
	
	public ImageLoaderTask(ImageWriter imageWriter_, ImageRetriever imageRetreiver_,
			ImageDecoder imageDecoder_, ImageSettings imageSettings_,
			String imageDirectoryLocation_, MemoryCache<ImageKey, File> diskCache_, OnImageLoad onImageLoadCallBack_){
		_imageDecoder = imageDecoder_;
		_imageRetreiver = imageRetreiver_;
		_imageWriter  = imageWriter_;
		_imageSettings = imageSettings_;
		_imageDirectoryLocation = imageDirectoryLocation_;
		_onImageLoadCallBack = onImageLoadCallBack_;
		_diskCache = diskCache_;
	}
	
	
	public ImageLoaderTask(ImageWriter imageWriter_, ImageRetriever imageRetreiver_,
			ImageDecoder imageDecoder_, ImageSettings imageSettings_,
			String imageDirectoryLocation_,  MemoryCache<ImageKey, File> diskCache_, OnImageLoad onImageLoadCallBack_, 
			ImageFailListenter taskListener_){
		_imageDecoder = imageDecoder_;
		_imageRetreiver = imageRetreiver_;
		_imageWriter  = imageWriter_;
		_imageSettings = imageSettings_;
		_imageDirectoryLocation = imageDirectoryLocation_;
		_onImageLoadCallBack = onImageLoadCallBack_;
		_taskListener = taskListener_;
		_diskCache = diskCache_;

	}
	
	@Override
	public void run() {

		try {
			
			Bitmap bitmap = tryLoadImageFromDisk();
			
			if(onLoadFinish(bitmap)){
				return;
			}
			
			bitmap = tryLoadImageFromNetwork();
			
			if(onLoadFinish(bitmap)){
				return;
			}
			
		} catch (IOException e) {
			sendImageFailReasonToCallback(ExceptionType.IOException, e);
			e.printStackTrace();
		} catch (URISyntaxException e) {
			sendImageFailReasonToCallback(ExceptionType.URISyntaxException, e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			sendImageFailReasonToCallback(ExceptionType.InterruptedException, e);
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			sendImageFailReasonToCallback(ExceptionType.OutOfMemoryError, e);
			e.printStackTrace();
		}
		
	}
	
	private void sendImageFailReasonToCallback(ExceptionType exceptionType_, Throwable throwable_){
		if(_taskListener!=null){
		_taskListener.onImageLoadFail(new FailedTaskReason(exceptionType_, throwable_), _imageSettings);
		}
	}
	
	/**
	 * Attempt to download the image from a network call adn write to disk
	 * 
	 * @return
	 * 		{@link Bitmap} of the final decoded image
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private Bitmap tryLoadImageFromNetwork() throws IOException,
			URISyntaxException {
		
		Bitmap imageLoadedFromNetwork = null;
		InputStream stream = _imageRetreiver.getStream(_imageSettings
					.getUrl());
		
		
		if(stream == null){
			Log.v(TAG, "Stream is null");
		}
		
		imageLoadedFromNetwork = _imageDecoder.decodeImage(stream, _imageSettings, true);
		_imageWriter.writeBitmapToDisk(_imageSettings,
					imageLoadedFromNetwork);

		return imageLoadedFromNetwork;
	}
	
	private boolean onLoadFinish(Bitmap bitmap_){
		if(bitmap_ != null){
			_onImageLoadCallBack.onImageLoadFinish(bitmap_);
			return true;
		}
		return false;
	}
	
	private Bitmap tryLoadImageFromDisk() throws IOException,
			URISyntaxException, InterruptedException {
		ImageKey imageKey = _imageSettings.getImageKey();

		File imageFile = _diskCache.getValue(imageKey);

		 if(imageFile == null){
		 imageFile = new File(_imageDirectoryLocation,
		 _imageSettings.getFinalFileName());
		 }

		if (imageFile != null) {
			L.v(TAG, THREAD_NAME + ": File loaded from disk with path: "
					+ imageFile.getAbsolutePath());

			if (imageFile.exists()) {
				L.v(TAG,
						THREAD_NAME
								+ "File exists and so decoding from the image from the disk: "
								+ imageFile.getAbsolutePath());
				Bitmap decodeImage = _imageDecoder.decodeImage(imageFile,
						_imageSettings, false);
				return decodeImage;
			}

		}
		return null;

	}

	
	


}
