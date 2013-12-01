package com.sun.activity;



import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


public class TestAdapter  extends FragmentPagerAdapter {
	public TestAdapter(FragmentManager fm) {
		super(fm);
		// TODO Auto-generated constructor stub
	}


	@Override
	    public Fragment getItem(int index) {

		Fragment frag = null;
		
	        switch (index) {
	        case 0:
	        	frag = new Fragment();
	        	return frag ;

	        	// Top Rated fragment activity
	        case 1:
	        	frag = new Fragment();

	        	return frag;
	        	
	        case 2:
	        	frag = new Fragment();

	        	return frag;
	        }
	 
	        return null;
	    }
	 
	    @Override
	    public int getCount() {
	        // get item count - equal to number of tabs
	        return 3;
	        }
	 
	
}
