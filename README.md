AsyncBackgroundViewPager
==============

Simple way to load a large background image that spans across a ViewPager widget. Scrolling between tabs automatiacally scrolls the background for you to. It also handels disk caching so that the same images will be loaded into memory much more efficiently rather than downloading them again. It also provides a away of automatically calculating the optimal image size for you as well :). This widget makes heavy use of the [Simple Image Cache](https://github.com/SundeepK/SIC/blob/master/README.md) for code reuse. I will be commiting a version with out the SIC once it is finished.


**Layout code**

Simply define a AsyncBackgroundViewPager:

  ``` xml
<?xml version="1.0" encoding="utf-8"?>


<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="fill_parent"
    android:layout_height="wrap_content"
    android:background="#ffffffff"
    android:descendantFocusability="beforeDescendants"
    android:focusable="true"
    android:focusableInTouchMode="true"
    android:orientation="vertical"
    tools:context=".MainActivity" >

    <com.sun.parallaxviewpage.core.AsyncBackgroundViewPager
        android:id="@+id/viewPager"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:background="#cc000000"
        android:scrollbarSize="1dp" >

        <android.support.v4.view.PagerTitleStrip
            android:id="@+id/pager_title_strip"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="top"
            android:background="#33b5e5"
            android:paddingBottom="4dp"
            android:paddingTop="4dp"
            android:textColor="#fff" />
    </com.sun.parallaxviewpage.core.AsyncBackgroundViewPager>

</LinearLayout>

  ```
  
 ``` java
@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    Button but = (Button) findViewById(R.id.buttonChangeImage);
	    
		_viewPage = (AsyncBackgroundViewPager) findViewById(R.id.viewPager);
		_viewPage.setAdapter(new TestAdapter(getSupportFragmentManager()));
		_viewPage.setExternalStorageDir("/storage/sdcard0/Pictures/test", 5);
		final ExecutorService threadPool = Executors.newFixedThreadPool(2);
		
				try {
			_viewPage.loadImage(
					new URI("https://si0.twimg.com/profile_background_images/378800000112087628/3a432a79828de732b54781d288d1fc14.jpeg")
					, 1, false);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
```


