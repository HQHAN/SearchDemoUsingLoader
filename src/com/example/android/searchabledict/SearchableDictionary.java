/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.searchabledict;

import android.app.Activity;
import android.app.ActionBar;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The main activity for the dictionary.
 * Displays search results triggered by the search dialog and handles
 * actions from search suggestions.
 */
public class SearchableDictionary extends Activity implements LoaderCallbacks<Cursor>{

	private static final String TAG = SearchableDictionary.class.getSimpleName();

    private TextView mTextView;
    private ListView mListView;
    
    private static final int LOADER_ID = 1;
    
    private String mQuery;
    private SimpleCursorAdapter mCusorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTextView = (TextView) findViewById(R.id.text);
        mListView = (ListView) findViewById(R.id.list);
        
        // Define the on-click listener for the list items
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Build the Intent used to open WordActivity with a specific word Uri
                Intent wordIntent = new Intent(getApplicationContext(), WordActivity.class);
                Uri data = Uri.withAppendedPath(DictionaryProvider.CONTENT_URI,
                                                String.valueOf(id));
                wordIntent.setData(data);
                startActivity(wordIntent);
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Because this activity has set launchMode="singleTop", the system calls this method
        // to deliver the intent if this activity is currently the foreground activity when
        // invoked again (when the user executes a search from this activity, we don't create
        // a new instance of this activity, so the system delivers the search intent here)
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // handles a click on a search suggestion; launches activity to show word
            Intent wordIntent = new Intent(this, WordActivity.class);
            wordIntent.setData(intent.getData());
            startActivity(wordIntent);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
           
            if (getLoaderManager().getLoader(LOADER_ID) == null) {
              Log.i(TAG, "+++ Initializing the new Loader... +++");
            } else {
              Log.i(TAG, "+++ Reconnecting with existing Loader (id '1')... +++");
            }
            
            if(intent.getStringExtra(SearchManager.QUERY).equalsIgnoreCase(mQuery) == false) {
            	mQuery = intent.getStringExtra(SearchManager.QUERY);
            	// if query changes, restart the loader so loader loads new cursor
            	 Log.i(TAG, "+++ Calling restartLoader()! +++");
            	getLoaderManager().restartLoader(LOADER_ID, null, this);
            }else {
            	// init loader to load the cursor that points to the search result
            	 Log.i(TAG, "+++ Calling initLoader()! +++");
            	getLoaderManager().initLoader(LOADER_ID, null, this);
            }
        	
            // handles a search query
//            String query = intent.getStringExtra(SearchManager.QUERY);
//            showResults(query);
        }
    }

    /**
     * Searches the dictionary and displays results for the given query.
     * @param query The search query
     */
    private void showResults(String query, Cursor c) {

    	// background cursor loading moved into Loader module
    	//        Cursor cursor = managedQuery(DictionaryProvider.CONTENT_URI, null, null,
    	//                                new String[] {query}, null);

        if (c == null) {
            // There are no results
        	if(mCusorAdapter != null)
        		mCusorAdapter.swapCursor(c);
        	
            mTextView.setText(getString(R.string.no_results, new Object[] {query}));
            
        } else {
            
            // Specify the columns we want to display in the result
            String[] from = new String[] { DictionaryDatabase.KEY_WORD,
                                           DictionaryDatabase.KEY_DEFINITION };

            // Specify the corresponding layout elements where we want the columns to go
            int[] to = new int[] { R.id.word,
                                   R.id.definition };

            // Create a simple cursor adapter for the definitions and apply them to the ListView
            if(mCusorAdapter == null) {
            	mCusorAdapter = new SimpleCursorAdapter(this, R.layout.result, c, from, to, 0);	
            	mListView.setAdapter(mCusorAdapter);
            } else {
            	// if simple cursor adapter exists, then swap the cursor
            	mCusorAdapter.swapCursor(c);
            }
            
            // Display the number of results
            int count = c.getCount();
            String countString = getResources().getQuantityString(R.plurals.search_results,
                                    count, new Object[] {count, query});
            mTextView.setText(countString);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		Log.i(TAG, "+++ creating the Loader... +++");
		
		CursorLoader loader = null;
		
		if(id == LOADER_ID) {
			loader = new DictionaryLoader(getApplicationContext());
			loader.setUri(DictionaryProvider.CONTENT_URI);
			loader.setSelectionArgs(new String[]{mQuery});
		}
		
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		
		Log.i(TAG, "+++ Loading finished ... +++");
		
		if(data == null ) {
			Log.i(TAG, "+++ cursor is null :( ... +++");
			
		} else if ( data.isClosed()) {
			Log.i(TAG, "+++ cursor is closed :( ... +++");
		}
			
		if(loader.getId() == LOADER_ID) {
			showResults(mQuery, data);
		} else {
			Log.i(TAG, "+++ unknown loader has done loading ! ... +++");
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
		Log.i(TAG, "+++ Reseting the Loader... +++");
		
		if(loader.getId() == LOADER_ID) {
			mCusorAdapter.swapCursor(null);
		} else {
			Log.i(TAG, "+++ unknown loader is being reset ! ... +++");
		}
		
	}
}
