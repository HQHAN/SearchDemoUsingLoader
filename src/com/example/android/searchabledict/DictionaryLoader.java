package com.example.android.searchabledict;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.util.Log;

public class DictionaryLoader extends CursorLoader {
	
	public DictionaryLoader(Context context) {
		super(context);
	}

	@Override
	public Cursor loadInBackground() {
		Log.i("DictionaryLoader", "loadInBackground() is called");
		
		return super.loadInBackground();
	}

}
