package com.sun.activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Button;

import com.example.asyncparallaxpagerview.R;
import com.sun.parallaxviewpage.core.AsyncBackgroundViewPager;
import com.sun.parallaxviewpage.core.api.ImageLoadTaskCallback;

public class MainAcitivty extends  FragmentActivity implements android.app.ActionBar.TabListener, ImageLoadTaskCallback{

	AsyncBackgroundViewPager _viewPage;
	private static  final String TAG = MainAcitivty.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    Button but = (Button) findViewById(R.id.buttonChangeImage);
		_viewPage = (AsyncBackgroundViewPager) findViewById(R.id.sundeepsBar);
		_viewPage.setAdapter(new TestAdapter(getSupportFragmentManager()));
		_viewPage.setExternalStorageDir("/storage/sdcard0/Pictures/twitterFiltrr", 5);
		ExecutorService threadPool = Executors.newFixedThreadPool(2);
		try {
			
			_viewPage.loadImage(
					new URI("https://si0.twimg.com/profile_background_images/378800000112087628/3a432a79828de732b54781d288d1fc14.jpeg")
					, 2, false, threadPool, new ImageLoadTaskCallback() {
						
						@Override
						public void onImageLoadStart(final Future<?> future_) {

						}
					});
		
 
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
				
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction arg1) {
		_viewPage.setCurrentItem(tab.getPosition(), true);
		
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onImageLoadStart(Future<?> future_) {
	}


}
