package com.sun.parallaxviewpage.core.api;

import java.util.concurrent.Future;

import com.sun.parallaxviewpage.imageloader.impl.ImageLoaderTask;

public interface ImageLoadTaskCallback {
	
	/**
	 * Use this interface if you want to receive a {@link Future} object of the {@link ImageLoaderTask} that was 
	 * kicked off to load the image asynchronously.
	 * @param future_
	 * 			{@link Future} task representing the {@link ImageLoaderTask} used to laod the image 
	 */
	public void onImageLoadStart(Future<?> future_);
	
}
