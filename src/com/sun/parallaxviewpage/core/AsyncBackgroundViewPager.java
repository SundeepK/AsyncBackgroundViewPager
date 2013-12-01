package com.sun.parallaxviewpage.core;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.sun.imageloader.cache.api.MemoryCache;
import com.sun.imageloader.cache.impl.DiskCache;
import com.sun.imageloader.core.ImageKey;
import com.sun.imageloader.core.ImageSettings;
import com.sun.imageloader.core.ImageWriter;
import com.sun.imageloader.core.api.FailedTaskReason;
import com.sun.imageloader.core.api.ImageFailListenter;
import com.sun.imageloader.core.api.ImageTaskListener;
import com.sun.imageloader.downloader.api.ImageRetriever;
import com.sun.imageloader.downloader.impl.ImageDownloader;
import com.sun.imageloader.downloader.impl.ImageRetrieverFactory;
import com.sun.imageloader.imagedecoder.api.ImageDecoder;
import com.sun.imageloader.imagedecoder.impl.SimpleImageDecoder;
import com.sun.imageloader.imagedecoder.utils.KeyUtils;
import com.sun.imageloader.imagedecoder.utils.L;
import com.sun.parallaxviewpage.core.api.ImageLoadTaskCallback;
import com.sun.parallaxviewpage.imageloader.impl.ImageLoaderTask;
import com.sun.parallaxviewpage.onimageload.api.OnImageLoad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;

public class AsyncBackgroundViewPager extends ViewPager implements OnImageLoad, ImageFailListenter{

	private Bitmap _parallaxBackgroundBitmap;
	private ImageDecoder _imageDecoder;
	private ImageRetriever _imageRetreiver;
	private ImageWriter _imageWriter;
	private static final String TAG = AsyncBackgroundViewPager.class.getName();
	private Rect _srcBounds = new Rect();
	private MemoryCache<ImageKey, File> _diskCache;
    private Handler _handler ;
    private int _height;
    private int _width;
    private URI _currentUri;
    private int _sampleSize;
    private boolean _isLoadingImage =false;
    private boolean _shouldFindOptimalSize;
    private String _imageStorageDir;
    private ExecutorService _executorService;
	private ImageLoadTaskCallback _imageLoadTaskcallBack;
	private boolean _shouldClearCurrentBackgroundImage;
    
	public AsyncBackgroundViewPager(Context context) {
		this(context, null);
	}


	public AsyncBackgroundViewPager(Context context_,AttributeSet attrs_){
		super(context_, attrs_);
		File internalStorage = getInternalDir(context_);
		_imageWriter = new ImageWriter(internalStorage);
		_diskCache = new DiskCache(internalStorage, 2, true);
		_imageDecoder = new SimpleImageDecoder();
		_handler = new Handler();
	}
	
	private File getInternalDir(Context context_){
		File storageDir = context_.getFilesDir();
		return storageDir;
	}
	
	/**
	 * Set the external storage location that will be used to read and write {@link Bitmap} images to.
	 * This allows for more efficient retrieval of images if the same image needs to be loaded again.
	 * 
	 * @param externalStorageLocation_
	 * 			{@link String} location of the external directory that will be used to cache images to
	 */
	public void setExternalStorageDir(String externalStorageLocation_, int maxFilesToCache_){
		_imageStorageDir = externalStorageLocation_;
		_imageWriter = new ImageWriter(externalStorageLocation_);
		_diskCache = new DiskCache(new File(externalStorageLocation_), maxFilesToCache_, true);
	}
	

	@Override
	protected void onDraw(Canvas canvas_) {
		if(_parallaxBackgroundBitmap!=null){
			_srcBounds.set(0, 0, getWidth() * getAdapter().getCount() , getHeight());
			canvas_.drawBitmap(_parallaxBackgroundBitmap, _srcBounds, _srcBounds, null);
		}
		super.onDraw(canvas_);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		_width = MeasureSpec.getSize(widthMeasureSpec);
		_height = MeasureSpec.getSize(heightMeasureSpec);
		if(_currentUri != null && _sampleSize > 0){
			startImageLoadTask(_currentUri, _sampleSize, _shouldFindOptimalSize, _executorService, _imageLoadTaskcallBack);
		}
	    this.setMeasuredDimension(_width/2, _height);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	/**
	 * boolean showing whether there is a {@link Thread} attempting to load an image in the background.
	 * 
	 * @return
	 * 		boolean value showing if there is a background {@link Thread} busy loading an image.
	 */
	public boolean isLoadingBackgroundImage(){
		return _isLoadingImage;
	}
	
	
	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		if(_currentUri != null && _sampleSize <= 0){
			startImageLoadTask(_currentUri, _sampleSize, _shouldFindOptimalSize, _executorService, _imageLoadTaskcallBack);
		}
		super.onLayout(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	protected void onPageScrolled(int position_, float positionOffset_, int positionOffsetPixels_) {
	    
	    super.onPageScrolled(position_,  positionOffset_,  positionOffsetPixels_);
	}
	
	
	public void recycle(){
		_parallaxBackgroundBitmap.recycle();
	}

	
	/**
	 * <p>
	 * Load a {@link Bitmap} image asynchronously and draw the image to the {@link ViewPager} background.
	 * {@link AsyncBackgroundViewPager} will automatically write the image to disk for you and reload the same image if
	 * you want to load the same {@link URI} again. Note that {@link AsyncBackgroundViewPager} will not manage the disk 
	 * for you and may need to manually delete images if you find images are consuming to much hard disk memory. This
	 * functionality may be added in later revisions.
	 * </p>
	 * 
	 * It is recommended that you manually set the sample size and leave the shouldFindOptimalSize_ flag to false.
	 * This will perform more efficiently and will consume less memory on the hard disk. 
	 * 
	 * @param url_
	 * 			the {@link URI} of the image to load
	 * @param sampleSize_
	 * 			the sample size of the image that the {@link Bitmap} will be re-sized to, this sample size will override any
	 * 			sample size that is internally calculated. Leave this to 1 if you want {@link AsyncBackgroundViewPager} to 
	 * 			automatically resize the {@link Bitmap} for you.
	 * @param shouldFindOptimalSize_
	 * 			set to true if you want {@link AsyncBackgroundViewPager} to automatically resize the {@link Bitmap} for you.
	 * 			
	 */
	public void loadImage(URI url_, int sampleSize_, boolean shouldFindOptimalSize_){
		startImageLoadTask( url_,  sampleSize_, shouldFindOptimalSize_, null, null);
	}
	
	
	/**
	 * <p>
	 * Load a {@link Bitmap} image asynchronously and draw the image to the {@link ViewPager} background.
	 * {@link AsyncBackgroundViewPager} will automatically write the image to disk for you and reload the same image if
	 * you want to load the same {@link URI} again. Note that {@link AsyncBackgroundViewPager} will not manage the disk 
	 * for you and may need to manually delete images if you find images are consuming to much hard disk memory. This
	 * functionality may be added in later revisions.
	 * </p>
	 * 
	 * It is recommended that you manually set the sample size and leave the shouldFindOptimalSize_ flag to false.
	 * This will perform more efficiently and will consume less memory on the hard disk. 
	 * 
	 * @param url_
	 * 			the {@link URI} of the image to load
	 * @param sampleSize_
	 * 			the sample size of the image that the {@link Bitmap} will be re-sized to, this sample size will override any
	 * 			sample size that is internally calculated. Leave this to 1 if you want {@link AsyncBackgroundViewPager} to 
	 * 			automatically resize the {@link Bitmap} for you.
	 * @param shouldFindOptimalSize_
	 * 			set to true if you want {@link AsyncBackgroundViewPager} to automatically resize the {@link Bitmap} for you.
	 * @param executorService_
	 * 			the {@link ExecutorService} on which to submit the {@link ImageLoaderTask} on.
	 * @return
	 * 		returns a {@link Future} to allow you to have control of the execution of the {@link ImageLoaderTask}, can return
	 * 		<code>null</code> if no {@link ExecutorService} service is supplied
	 * 			
	 */
	public void loadImage(URI url_, int sampleSize_, boolean shouldFindOptimalSize_,
			ExecutorService executorService_, ImageLoadTaskCallback callBack_){
		startImageLoadTask( url_,  sampleSize_, shouldFindOptimalSize_, executorService_, callBack_);
	}
	
	/**
	 * Lets you register the {@link ImageLoaderTask} on your very own {@link ExecutorService} so that you can have full control
	 * over the task
	 * @param url_
	 * 			the {@link URI} of the image to load
	 * @param sampleSize_
	 * 			the sample size of the image that the {@link Bitmap} will be re-sized to, this sample size will overide any
	 * 			sample size that is internally calculated. Leave this to 1 if you want {@link AsyncBackgroundViewPager} to 
	 * 			automatically resize the {@link Bitmap} for you.
	 * @param shouldFindOptimalSize_
	 * 			set to true if you want {@link AsyncBackgroundViewPager} to automatically resize the {@link Bitmap} for you.
	 * @param executorService_
	 * 			the {@link ExecutorService} on which to run the {@link ImageLoaderTask} on 
	 * @param imageLoadTaskcallBack_
	 * 		{@link ImageLoadTaskCallback} that returns a {@link Future} to allow you to have control of the execution 
	 * 		of the {@link ImageLoaderTask}
	 *  	
	 */
	private synchronized void startImageLoadTask(URI url_, int sampleSize_, 
			boolean shouldFindOptimalSize_, ExecutorService executorService_, ImageLoadTaskCallback imageLoadTaskcallBack_){
	
		if(_isLoadingImage){
			return;
		}
		
		_executorService = executorService_;
		_imageLoadTaskcallBack = imageLoadTaskcallBack_ ;

		if(!isValidDimensions(url_,sampleSize_ , shouldFindOptimalSize_)){
			return;
		}
		_isLoadingImage = true;

		if (_executorService != null) {
			Runnable imageRunnable = getImageLoadTask(url_, sampleSize_, shouldFindOptimalSize_);
			 Future<?> future = executorService_.submit(imageRunnable);
			_imageLoadTaskcallBack.onImageLoadStart(future);
		} else {
			 Runnable imageRunnable = getImageLoadTask(url_, sampleSize_, shouldFindOptimalSize_);
			 Thread t = new Thread(imageRunnable);
			 t.start();
		}
	}
	
	private boolean isValidDimensions(URI url_, int sampleSize_, boolean shouldFindOptimalSize_){
		if (_width <= 0 && _height <= 0) {
			_shouldFindOptimalSize = shouldFindOptimalSize_;
			_currentUri = url_;
			_sampleSize = sampleSize_;
			return false;
		}
		return true;
	}
	
	private Runnable getImageLoadTask(URI url_, int sampleSize_, boolean shouldFindOptimalSize_){
		ImageSettings settings = getImageSettingsForDecode(url_,sampleSize_,shouldFindOptimalSize_ );
		_imageRetreiver = ImageRetrieverFactory.getImageRetriever(url_);
			return new ImageLoaderTask(_imageWriter, _imageRetreiver, 
						_imageDecoder, settings,_imageStorageDir
						,_diskCache, this, this);
	}
	
	private ImageSettings getImageSettingsForDecode(URI uri_, int sampleSize_, boolean shouldFindOptimalSize_){
		ImageKey imageKey = getImageKey(uri_, sampleSize_);
		if(shouldFindOptimalSize_){
			return new ImageSettings(uri_, null, imageKey, 
					CompressFormat.JPEG, Config.ARGB_8888, 50, (_width* getAdapter().getCount()), _height, shouldFindOptimalSize_);
		}else{
			return new ImageSettings(uri_, null, imageKey, CompressFormat.JPEG, Config.ARGB_8888, 50);

		}
	}
  
	private ImageKey getImageKey(URI uri_, int sampleSize){
		int key = KeyUtils.getPathKey(uri_);
		return new ImageKey(key,sampleSize);
	}
 
	@Override
	public void onImageLoadFinish(Bitmap loadedBitmap_) {
		_isLoadingImage = false;
		_parallaxBackgroundBitmap = loadedBitmap_;
		_handler.post(new Runnable() {

				@Override
				public void run() {
					AsyncBackgroundViewPager.this.invalidate();
				}
			});
	}

	@Override
	public void onImageLoadFail(FailedTaskReason arg0, ImageSettings arg1) {
		Log.v(TAG, "Image failed to load");
			_isLoadingImage = false;
	}

}
