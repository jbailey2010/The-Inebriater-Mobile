package com.bevinisaditch.theinebriator.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.bevinisaditch.theinebriator.Home;
import com.bevinisaditch.theinebriator.Loading;
import com.bevinisaditch.theinebriator.ClassFiles.Drink;
import com.bevinisaditch.theinebriator.ClassFiles.Ingredient;
import com.bevinisaditch.theinebriator.ClassFiles.TermFrequency;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class handles the database operations for term frequency, which is used in the search engine.
 * 
 * @author michael
 *
 */
@SuppressLint("DefaultLocale")
public class TermFrequencyDatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 3;
 
    // Database Name
    private static final String DATABASE_NAME = "termFrequency";
 
    // Contacts table name
    private static final String TABLE_TERM_FREQ = "term_freq";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TERM = "term";
    private static final String KEY_FREQ = "frequency";
 
    public TermFrequencyDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_TERM_FREQ + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_TERM + " TEXT,"
                + KEY_FREQ + " REAL" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);

        Log.d("TermFrequencyDatabaseHandler", "Populating Term Frequency DB");
        // Populate DB
        List<String> drinkNames = Loading.drinkNames;
        if (drinkNames != null && drinkNames.size() > 0) {
        	populateDatabase(db, drinkNames, Loading.ingrNames);
        }
        Log.d("TermFrequencyDatabaseHandler", "Finished populating Term Frequency DB");
	}
	
	
	/**
	 * Takes a list of drinks and calculates the frequencies for each term.
	 * Then inserts in to the database.
	 * 
	 * @param allDrinks - ArrayList of drinks to break down into terms
	 */
	public void populateDatabase(SQLiteDatabase db, List<String> drinkNames, List<String> list) {
		HashMap<String, Float> termFreq = new HashMap<String, Float>();
        Integer totalTermCount = 0;
        for (String name : drinkNames) {
        	name = name.toLowerCase();
        	String[] terms = name.split("\\s+");
        	Float count;
        	for (String term : terms) {        		
	        	count = termFreq.get(term);
	        	if (count != null) {
	        		termFreq.put(term, count + 1);
	        	} else {
	        		termFreq.put(term, 1f);
	        	}
	        	totalTermCount += 1;
        	}
        }
        for(String name : list){
        	name = name.toLowerCase();
        	String [] terms = name.split("\\s+");
        	Float count;
        	for (String term : terms) {
	        	count = termFreq.get(term);
	           	if (count != null) {
	           		termFreq.put(term, count + 1);
	           	} else {
	           		termFreq.put(term, 1f);
	           	}
	           	totalTermCount += 1;
        	}
        }
        
        //Calculate the frequency for term
        for (Entry<String, Float> entry : termFreq.entrySet()) {
        	termFreq.put(entry.getKey(), entry.getValue()/totalTermCount);
        }
		        
        //Add each entry to the database
        addAllTermFreq(termFreq, db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TERM_FREQ);
         
        // Create tables again
        onCreate(db);
		
	}
	
	public void addAllTermFreq(HashMap<String, Float> termMap, SQLiteDatabase db) {
		try{
    		db.beginTransaction();
    		for(String term : termMap.keySet()){
    			Float freq = termMap.get(term);
    			ContentValues values = new ContentValues();
    			values.put(KEY_TERM, term);
    			values.put(KEY_FREQ, freq);
    			
    			db.insert(TABLE_TERM_FREQ, null, values);
    		}
    		db.setTransactionSuccessful();
    	} catch (SQLException e) {}
    	finally{
    		db.endTransaction();
    	}
	}
	
	/**
	 *  Add a term frequency instance to the database
	 *  Overwritten for initial population of database
	 * 
	 * @param termFreq instance of term frequency
	 * @return id of the row
	 */
	public Long addTermFreq(SQLiteDatabase db, TermFrequency termFreq) {
		 
	    ContentValues values = new ContentValues();
	    values.put(KEY_TERM, termFreq.getTerm());
	    values.put(KEY_FREQ, termFreq.getFrequency());
	 
	    long id = db.insert(TABLE_TERM_FREQ, null, values);
	    return id;
	}
	 
	/**
	 * Get an instance of term frequency by name of the term
	 * 
	 * @param term - name of the term
	 * @return TermFrequency instance
	 */
	public TermFrequency getTermFrequency(String term) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		//Query for termFrqe
	    Cursor cursor = db.query(TABLE_TERM_FREQ, new String[] { KEY_ID,
	            KEY_TERM, KEY_FREQ }, KEY_TERM + "=?",
	            new String[] { term }, null, null, null, null);
	    
	    if (cursor != null) {
	        cursor.moveToFirst();
	    }
	    
	    if (cursor.getCount() == 0) {
	    	cursor.close();
	    	return null;
	    }
	    
	    //Term into data class
	    TermFrequency termFreq = new TermFrequency(cursor.getLong(0),
	            cursor.getString(1), cursor.getFloat(2));
	    
	    cursor.close();
	    
	    return termFreq;
	}
	
	/**
	 * Overloaded method to get the term frequency by id
	 * 
	 * @param id - id of the row
	 * @return TermFrequency Instance
	 */
	public TermFrequency getTermFrequency(Long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		//Query for termFrqe
	    Cursor cursor = db.query(TABLE_TERM_FREQ, new String[] { KEY_ID,
	            KEY_TERM, KEY_FREQ }, KEY_ID + "=?",
	            new String[] { String.valueOf(id) }, null, null, null, null);
	    
	    if (cursor != null) {
	        cursor.moveToFirst();
	    }
	    
	    if (cursor.getCount() == 0) {
	    	cursor.close();
	    	return null;
	    }
	 
	    //Term into data class
	    TermFrequency termFreq = new TermFrequency(cursor.getLong(0),
	            cursor.getString(1), cursor.getFloat(2));
	    
	    cursor.close();
	    
	    return termFreq;
	}
	 
	/**
	 * Query for all the term frequencies in the database
	 * 
	 * @return ArrayList of all the term frequency objects
	 */
	public ArrayList<TermFrequency> getAllTermFreqs() {
		ArrayList<TermFrequency> termFreqList = new ArrayList<TermFrequency>();
	    
		// Select All Query
	    String selectQuery = "SELECT  * FROM " + TABLE_TERM_FREQ;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	            TermFrequency termFreq = new TermFrequency();
	            termFreq.setID(cursor.getLong(0));
	            termFreq.setTerm(cursor.getString(1));
	            termFreq.setFrequency(cursor.getFloat(2));
	            
	            termFreqList.add(termFreq);
	        } while (cursor.moveToNext());
	    }
	 
	    cursor.close();
	    
	    return termFreqList;
		
	}
	 
	/**
	 * Returns the number of rows in the Term Frequency Table
	 * 
	 * @return Number of rows in Term Frequency Table 
	 */
	public Integer getTermFreqCount() {
		String countQuery = "SELECT  * FROM " + TABLE_TERM_FREQ;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        Integer count = cursor.getCount();
        cursor.close();
 
        return count;
	}
	
	/**
	 * Updates the term frequency that already exists
	 * 
	 * @param termFreq - Term frqeuency you want to update
	 * @return The number of rows that were updated
	 */
	public int updateTermFreq(TermFrequency termFreq) {
		SQLiteDatabase db = this.getWritableDatabase();
		 
	    ContentValues values = new ContentValues();
	    values.put(KEY_TERM, termFreq.getTerm());
	    values.put(KEY_FREQ, termFreq.getFrequency());
	 
	    // updating row
	    return db.update(TABLE_TERM_FREQ, values, KEY_ID + " = ?",
	            new String[] { String.valueOf(termFreq.getID()) });
		
	}
	 
	/**
	 * Deletes a contact from the database table
	 * 
	 * @param termFreq
	 */
	public void deleteTermFreq(TermFrequency termFreq) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_TERM_FREQ, KEY_ID + " = ?",
	            new String[] { String.valueOf(termFreq.getID()) });
	    db.close();
	}
	
}
