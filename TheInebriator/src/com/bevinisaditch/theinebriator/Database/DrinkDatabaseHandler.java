package com.bevinisaditch.theinebriator.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.bevinisaditch.theinebriator.ClassFiles.Drink.Rating;
import com.bevinisaditch.theinebriator.ClassFiles.*;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Class for handling the database of drinks.
 * @author termaat1
 *
 */
public class DrinkDatabaseHandler extends SQLiteOpenHelper 
{
		 
	    // All Static variables
	    // Database Version
	    private static final int DATABASE_VERSION = 2;
	 
	    // Database Name
	    private static final String DATABASE_NAME = "DrinksAndIngredients";
	    
	    private Context con;
	    
	    //Names of text files as found in assets to read data from
	    private static final String DRINK_FILE_NAME = "drinkData.txt";
	    private static final String MATCH_FILE_NAME = "matchData.txt";
	    private static final String PAIR_FILE_NAME = "pairData.txt";
	    
	    private Set<Integer> nullSet = new HashSet<Integer>();
	 	  
	    /**
	     * just calls super constructor
	     * @param context
	     */
	    public DrinkDatabaseHandler(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION); 
	        con = context; 
	    } 
	    
	    /**
	     * Deletes the old tables and re-reads from text files to populate them again.
	     */
	    public void reCreateTables()
	    { 
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	deleteTablesIfExist(db);
	    	onCreate(db);
	    }
	 
	    /**
	     * Creates tables and inserts the drinks, ingredients, and matchings into database
	     */
	    public void onCreate(SQLiteDatabase db) {
	        String CREATE_DRINKS_TABLE = "CREATE TABLE DRINKS" +
	        		"(ID INTEGER PRIMARY KEY," +
					"NAME           TEXT       NOT NULL," +
					"RATING         INT        NOT NULL," +
					"INSTRUCTIONS   TEXT       NOT NULL)" ;
	        db.execSQL(CREATE_DRINKS_TABLE);
	        String CREATE_PAIRS_TABLE = "CREATE TABLE INGREDIENTS" +
					"(ID INTEGER PRIMARY KEY," +
					"NAME           TEXT      NOT NULL)";
	        db.execSQL(CREATE_PAIRS_TABLE);
	        
	        //TODO: Quantity and Units should be in the 'INGREDIENTS' table - Mike
	        String CREATE_MATCHINGS_TABLE = "CREATE TABLE MATCHINGS" +
					"(ID INTEGER PRIMARY KEY," +
					"DRINKID        INT        NOT NULL," +
					"INGREDIENTID   INT        NOT NULL," +
					"QUANTITY       TEXT," +
					"UNITS          TEXT)";
	        db.execSQL(CREATE_MATCHINGS_TABLE);

	        readDrinks(db);
		    readMatchings(db);
		    readPairs(db);
	    }
	 
	    /**
	     * Deletes and repopulates tables if database was upgraded
	     */
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        // Drop older table if existed
	        deleteTablesIfExist(db);
	 
	        // Create tables again
	        onCreate(db);
	    }

	    /**
	     * Deletes tables from db if they already exist
	     * @param db this database
	     */
		private void deleteTablesIfExist(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS DRINKS");
	        db.execSQL("DROP TABLE IF EXISTS INGREDIENTS");
	        db.execSQL("DROP TABLE IF EXISTS MATCHINGS");
		}
	    
		/**
		 * Thumbs up the drink with given ID
		 * @param drinkID
		 */
		public void thumbsUpDrink(Long drinkID)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			String updateText = "UPDATE DRINKS " + "SET RATING = 1 " + "WHERE ID = " + drinkID;
			db.execSQL(updateText);
		}
		
		/**
		 * Sets the rating to no thumbs up nor thumbs down for the drink with the given id
		 * @param drinkID
		 */
		public void thumbsNullDrink(Long drinkID)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			String updateText = "UPDATE DRINKS " + "SET RATING = 0 " + "WHERE ID = " + drinkID;
			db.execSQL(updateText);
		}
		
		/**
		 * Thumbs down the drink with given ID
		 * @param drinkID
		 */
		public void thumbsDownDrink(Long drinkID)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			String updateText = "UPDATE DRINKS " + "SET RATING = -1 " + "WHERE ID = " + drinkID;
			db.execSQL(updateText);
		}
		
		/**
		 * Sets a drink's rating to the given rating
		 * @param drinkID ID of drink that needs a rating change
		 * @param rating Rating to which the drink's rating should be changed
		 */
		public void setDrinkRating(Long drinkID, Rating rating)
		{
			if (rating == Rating.THUMBSUP)
				thumbsUpDrink(drinkID);
			else if (rating == Rating.THUMBSDOWN)
				thumbsDownDrink(drinkID);
			else
				thumbsNullDrink(drinkID);
		}
		
		
		/**
		 * This function takes a drink and add its to the database
		 * 
		 * @param drink - Drink to be added
		 * 
		 * @return id - row of the drink that was just added
		 */
		public long addDrink(Drink drink) {
			SQLiteDatabase db = this.getWritableDatabase();
						
			//Add drink
			ContentValues drinkValues = new ContentValues();
			drinkValues.put("NAME", drink.getName());
			drinkValues.put("RATING", ratingToInt(drink.getRating()));
			drinkValues.put("INSTRUCTIONS", drink.getInstructions());
			long drinkID = db.insert("DRINKS", null, drinkValues);
			
			if (drinkID == -1) {
				System.out.println("Error inserting drink");
			}
			
			ArrayList<Ingredient> ings = drink.getIngredients();
			
			for (Ingredient ing : ings) {
				//Add ingredient
				ContentValues ingValues = new ContentValues();
				ingValues.put("NAME", ing.getName());
		    	long ingID = db.insert("INGREDIENTS", null, ingValues);
		    	if (ingID == -1) {
					System.out.println("Error inserting ingredient");
				}
		    	
		    	//Add matching
		    	ContentValues values = new ContentValues();
		    	values.put("DRINKID", drinkID);
		    	values.put("INGREDIENTID", ingID);
		    	values.put("QUANTITY", ing.getQuantity());
		    	values.put("UNITS", ing.getUnits());
		    	long matchingID = db.insert("MATCHINGS", null, values);
		    	if (matchingID == -1) {
					System.out.println("Error inserting matching");
				}
			}
			
			return drinkID;
			
		}
		
	    /**
	     * Adds a drink to the database, ignoring its ingredients.
	     * @param drink
	     */
	    public void addDrinkWithoutIngredients(Drink drink)
	    {
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	 
	        ContentValues values = new ContentValues();
	        values.put("ID", drink.getId()); 
	        values.put("NAME", drink.getName());
	        values.put("RATING", ratingToInt(drink.getRating()));
	        values.put("INSTRUCTIONS", drink.getInstructions());
	     
	        // Inserting Row
	        db.insert("DRINKS", null, values);
	    }
	    
	    /**
	     * Adds all drinks to the database, unless it's empty
	     * 
	     * @param id ID of drink to add
	     * @param name Name of drink to add
	     * @param rating Rating of drink to add
	     * @param instructions Instructions of drink to add
	     * @param db This database
	     */
	    public void addDrinkByVars(List<Object> drinkData, SQLiteDatabase db)
	    {
	    	try{
	    		db.beginTransaction();
	    		for(int i = 0; i < drinkData.size(); i++){
	    			if(drinkData.get(i+1) != null && ((String)drinkData.get(i+1)).length() > 0){
		    	    	ContentValues values = new ContentValues();
		    	    	values.put("ID", (Integer) drinkData.get(i));
		    	    	values.put("NAME", (String)drinkData.get(++i));
		    	    	values.put("RATING", (Integer)drinkData.get(++i));
		    	    	values.put("INSTRUCTIONS", (String)drinkData.get(++i));
		    	    	db.insert("DRINKS", null, values);
	    			}
	    			else{
	    				Integer id = (Integer)drinkData.get(i);
    					nullSet.add(id);
	    				i += 3;
	    			}
	    		}
	    		db.setTransactionSuccessful();
	    	} catch (SQLException e) {}
	    	finally{
	    		db.endTransaction();
	    	}
	    }
	    
	    
	    /**
	     * Adds all matchings to the database, unless they 
	     * correspond to an empty drink.
	     * 
	     * @param match
	     */
	    public void addMatching(List<Matching> matchings, SQLiteDatabase db)
	    {
	    	try{
	    		db.beginTransaction();
	    		for(Matching match : matchings){
	    			if(!nullSet.contains(match.drinkID)){
		    	    	ContentValues values = new ContentValues();
		    	    	values.put("ID", match.matchID);
		    	    	values.put("DRINKID", match.drinkID);
		    	    	values.put("INGREDIENTID", match.ingredientID);
		    	    	values.put("QUANTITY", match.quantity);
		    	    	values.put("UNITS", match.units);
		    	    	db.insert("MATCHINGS", null, values);
	    			}
	    		}
	    		db.setTransactionSuccessful();
	    	} catch (SQLException e) {}
	    	finally{
	    		db.endTransaction();
	    	}
	    }
	    
	    /**
	     * Adds all IngredientIDPairs to the database.
	     * @param pair
	     */
	    public void addPair(List<IngredientIDPair> pairs, SQLiteDatabase db)
	    {
	    	try{
	    		db.beginTransaction();
		    	for(IngredientIDPair pair : pairs){
			    	ContentValues values = new ContentValues();
			    	values.put("ID", pair.id);
			    	values.put("NAME", 	pair.name);
			    	db.insert("INGREDIENTS", null, values);
		    	}
		    	db.setTransactionSuccessful();
	    	} catch (SQLException e) {}
	    	finally{
	    		db.endTransaction();
	    	}
	    }


	    /**
	     * Converts a rating to an int
	     * @param rating The rating to convert
	     * @return -1 if thumbs down 0 if thumbs null and +1 if thumbs up
	     */
	    private int ratingToInt(Rating rating)
	    {
	    	if (rating == Rating.THUMBSNULL)
	    		return 0;
	    	else if (rating == Rating.THUMBSUP)
	    		return 1;
	    	else
	    		return -1;
	    }
	    
	    

	    /**
	     * Gets a list of drinks corresponding to the list of drink IDs
	     * @param ids IDs of drinks to get
	     * @param matches Relevant matchings
	     * @param pairs Relevant IngredientIDPairs
	     * @param db This database
	     * @return Drinks with given IDs
	     */
	    private Drink getDrinkByID(int id, SQLiteDatabase db)
	    {
    		String sql = "SELECT * FROM DRINKS WHERE ID = " + id;
    		Cursor cursor = db.rawQuery(sql, null);
    		Drink drink = null;
    		if (cursor.moveToFirst())
    		{
    			long index = cursor.getInt(0);
				String name = cursor.getString(1);
				Drink.Rating rating =  intToRating(cursor.getInt(2));
				String instructions = cursor.getString(3);
				ArrayList<Ingredient> ingredients = getIngredientsForDrinkID(index);
				drink = new Drink(name, rating, ingredients, instructions, index);
    		}
    		cursor.close();
	       	return drink;
	    }
	    
		
	    /**
	     * Gets all matchings in the database
	     * @return
	     */
	    public ArrayList<Matching> getAllMatchings()
	    {
	    	ArrayList<Matching> matchings = new ArrayList<Matching>();
	        // Select All Query
	        String selectQuery = "SELECT * FROM MATCHINGS";
	     
	        SQLiteDatabase db = this.getWritableDatabase();
	        Cursor cursor = db.rawQuery(selectQuery, null);
	     
	        // looping through all rows and adding to list
	        if (cursor.moveToFirst()) {
	            do {
	                Matching matching = new Matching();
	                matching.matchID = cursor.getInt(0);
	                matching.drinkID = cursor.getInt(1);
	                matching.ingredientID = cursor.getInt(2);
	                matching.quantity = cursor.getString(3);
	                matching.units = cursor.getString(4);
	                matchings.add(matching);
	            } while (cursor.moveToNext());
	        }
	        
	        cursor.close();
	     
	        return matchings;
	    }
	    
	    /**
	     * Gets all IngredientIDPairs in the database
	     * @return
	     */
	    public ArrayList<IngredientIDPair> getAllPairs()
	    {
	    	ArrayList<IngredientIDPair> pairs = new ArrayList<IngredientIDPair>();
	        // Select All Query
	        String selectQuery = "SELECT * FROM INGREDIENTS";
	     
	        SQLiteDatabase db = this.getWritableDatabase();
	        Cursor cursor = db.rawQuery(selectQuery, null);
	     
	        // looping through all rows and adding to list
	        if (cursor.moveToFirst()) {
	            do {
	            	IngredientIDPair pair = new IngredientIDPair(cursor.getInt(0), cursor.getString(1));
	                pairs.add(pair);
	            } while (cursor.moveToNext());
	        }
	        
	        cursor.close();
	     
	        return pairs; 
	    }
	    
	    /**
	     * Gets all drinks in the database with ingredients
	     * @return
	     */
	    public ArrayList<Drink> getAllDrinks()
	    {
	    	ArrayList<Drink> drinks = new ArrayList<Drink>();
	    	ArrayList<Matching> allMatches = getAllMatchings();
			ArrayList<IngredientIDPair> allPairs = getAllPairs();
			String selectQuery = "SELECT * FROM DRINKS;" ;
			SQLiteDatabase db = this.getWritableDatabase();
			Cursor cursor = db.rawQuery(selectQuery, null);
			
			if (cursor.moveToFirst())
			{
				do
				{
					Long id = cursor.getLong(0);
					String name = cursor.getString(1);
					Drink.Rating rating =  intToRating(cursor.getInt(2));
					String instructions = cursor.getString(3);
					ArrayList<Ingredient> ingredients = getIngredientsForDrinkID(id, allMatches, allPairs);

					Drink currDrink = new Drink(name, rating, ingredients, instructions, id);
					//Add currDrink to database
					drinks.add(currDrink);
				} while (cursor.moveToNext());
			}
			
			cursor.close();
			return drinks;
	    }
	    
	    /**
	     * Reads all of the drink names from file, for the 
	     * sake of the drink search dropdown primarily
	     * 
	     * @return - the list of drink names, uniquely
	     */
	    public List<String> getDrinkNames(){
	    	List<String> names = new ArrayList<String>();
	    	String selectQuery = "SELECT NAME FROM DRINKS";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			names.add(cursor.getString(0));
	    		} while(cursor.moveToNext());
	    	}
	    	return names;
	    }
	    
	    /**
	     * Reads all of the ingredient names from file, for the 
	     * sake of the search dropdown primarily
	     * 
	     * @return - the list of ingredient names, uniquely
	     */
	    public List<String> getIngredientNames(){
	    	List<String> names = new ArrayList<String>();
	    	String selectQuery = "SELECT NAME FROM INGREDIENTS";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			names.add(cursor.getString(0));
	    		} while(cursor.moveToNext());
	    	}
	    	return names;
	    }
	    
	    /**
	     * Gets the rating of a drink, given the id of that drink
	     * 
	     * @param id - the id of the drink to be checked
	     * @return the rating of the drink
	     */
	    public Rating getDrinkRating(Long id){
	    	Rating rating = Rating.THUMBSNULL;
	    	String selectQuery = "SELECT RATING FROM DRINKS WHERE ID = '" + id + 
	    			"'";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			rating = intToRating(cursor.getInt(0));
	    		} while(cursor.moveToNext());
	    	}
	    	return rating;
	    }
	    
	    /**
	     * Gets all of the ids from the drinks database. Useful since
	     * they may be a non-continuous set since missing data in drinks
	     * means a skipped id
	     * 
	     * @return the list of ids for drinks
	     */
	    public List<Integer> getIdsList(){
	    	List<Integer> indices = new ArrayList<Integer>();
	    	String selectQuery = "SELECT ID FROM DRINKS";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			indices.add(cursor.getInt(0));
	    		} while(cursor.moveToNext());
	    	}
	    	return indices;
	    }
	    
	    /**
	     * Gets the drinks rated with a given rating from 
	     * the database 
	     * 
	     * @param rating - the thumbs up/down/null rating of the drink
	     * @return the list of drinks rated as such
	     */
	    public List<Drink> getRatedDrinks(Rating rating){
	    	List<Drink> drinks = new ArrayList<Drink>();
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	List<Integer> ratedIds = getDrinkIDByRating(rating);
	    	for(Integer id : ratedIds){
	    		drinks.add(getDrinkByID(id, db));
	    	}
	    	return drinks;
	    }
	    
	    /**
	     * Gets all of the ids of drinks that have the specified 
	     * rating
	     * 
	     * @param rating - the rating, beit thumbs up, down, or null
	     * @return the list of ids of those drinks
	     */
	    public List<Integer> getDrinkIDByRating(Rating rating){
	    	List<Integer> indices = new ArrayList<Integer>();
	    	int ratingInt = ratingToInt(rating);
	    	String selectQuery = "SELECT ID FROM DRINKS WHERE RATING = '" + ratingInt + "'";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			indices.add(cursor.getInt(0));
	    		} while(cursor.moveToNext());
	    	}
	    	return indices;
	    }
	    
	    /**
	     * Given an index, it gets that index-th drink id (since they may be 
	     * non-continuous due to missing data), and uses that to grab a drink and
	     * return it. 
	     * 
	     * @param index - the index of the drink id to get
	     * @return the random drink
	     */
	    public Drink getRandomDrink(int index){
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	int randIndex = getIdsList().get(index);
	    	return getDrinkByID(randIndex, db);
	    }
	    
	    /**
	     * Gets the drink ID of a drink defined by the given name 
	     * and instructions. Note, this may not be airtight, and may
	     * need revisiting. It was chosen because it should be faster than 
	     * adding in ingredients to the equation.
	     * 
	     * @param name the name of the drink
	     * @param instr the instructions to make it
	     * @return the id of the drink
	     */
	    public Long getDrinkId(String name, String instr){
	    	long id = -1;
	    	String selectQuery = "SELECT ID FROM DRINKS WHERE NAME = '" + name + 
	    			"' AND INSTRUCTIONS = '" + instr + "'";
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	if (cursor.moveToFirst()) {
	    		do {
	    			id = cursor.getInt(0);
	    		} while(cursor.moveToNext());
	    	}
	    	return id;
	    }
	    
	    /**
	     * A janky helper to determine how unique the instructions plus 
	     * drink identifier is. It may be useful again if we add drinks, 
	     * but as of the original set, there are 13232 drinks, and it identifies
	     * 13228 uniquely by that classification.
	     */
		public void isUnique(){
			String selectTotal = "SELECT count(ID) FROM DRINKS";
			int count = -1;
			SQLiteDatabase db = this.getWritableDatabase();
	    	Cursor cursor = db.rawQuery(selectTotal, null);
	    	if (cursor.moveToFirst()){
	    		do {
	    			count = cursor.getInt(0);
	    		} while(cursor.moveToNext());
	    	}
	    	System.out.println("Total count: " + count);
	    	
	    	String selectUnique = "SELECT DISTINCT NAME,INSTRUCTIONS FROM DRINKS";
	    	cursor = db.rawQuery(selectUnique, null);
	    	count = 0;
	    	if (cursor.moveToFirst()){
	    		do {
	    			count++;
	    			cursor.getInt(0);
	    		} while(cursor.moveToNext());
	    	}
	    	System.out.println("Unique count: " + count);
		}
	    
	    
	    /**
	     * Gets relevant drinks by name
	     * @param terms The terms to which the drinks must be relevant
	     * @return The drinks relevant to terms
		**/
	    public ArrayList<Drink> getRelevantDrinksByName(ArrayList<String> terms) {
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	ArrayList<Matching> allMatches = getAllMatchings();
	    	ArrayList<IngredientIDPair> allPairs = getAllPairs();
	    	
	    	String selectQuery = "SELECT * FROM DRINKS WHERE ";
	    	
	    	//Creates query for database
	    	if (terms != null && terms.size() > 0) {
	    		selectQuery += "NAME LIKE '% " + terms.get(0) + " %'";
	    		selectQuery += " OR NAME LIKE '" + terms.get(0) + " %'";
	    		selectQuery += " OR NAME LIKE '% " + terms.get(0) + "'";
	    		terms.remove(0);
	    	}
	    	
	    	for (String term : terms) {
	    		selectQuery += " OR NAME LIKE '% " + term + " %'";
	    		selectQuery += " OR NAME LIKE '" + term + " %'";
	    		selectQuery += " OR NAME LIKE '% " + term + "'";
	    		
	    	}
	    	
	    	Cursor cursor = db.rawQuery(selectQuery, null);
	    	
	    	ArrayList<Drink> drinks = new ArrayList<Drink>();
			if (cursor.moveToFirst())
			{
				do
				{
					long id = cursor.getLong(0);
					String name = cursor.getString(1);
					Drink.Rating rating =  intToRating(cursor.getInt(2));
					String instructions = cursor.getString(3);
					ArrayList<Ingredient> ingredients = getIngredientsForDrinkID(id, allMatches, allPairs);

					Drink currDrink = new Drink(name, rating, ingredients, instructions, id);
					drinks.add(currDrink);
				} while (cursor.moveToNext());
			}
			cursor.close();
			return drinks;

	    	
	    }
	    
	    /**
	     * Gets the drinks that contain listed ingredients
	     * @param ingredients listed ingredients
	     * @return drinks that contain ingredients
	     */
	    public ArrayList<Drink> getRelevantDrinksByIngredient(ArrayList<String> ingredients)
	    {
	    	//TODO: Fix this
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	ArrayList<Drink> drinks = new ArrayList<Drink>();
	    	
	    	for (String ingredient : ingredients ) {
	    		
	    		//Get all Drinks that have the required ingredient
	    		String query = "SELECT d.ID, d.NAME, d.RATING, d.INSTRUCTIONS "
	    				 + "FROM Drinks d "
	    				 + "JOIN Matchings m on d.ID = m.DRINKID "
	    				 + "JOIN Ingredients i on i.ID = m.INGREDIENTID "
	    				 + "WHERE i.NAME LIKE '% " + ingredient + " %' "
	    				 + "or i.NAME LIKE '" + ingredient + " %' "
	    				 + "or i.NAME LIKE '% " + ingredient + "' "
	    				 + "or LOWER( i.NAME ) = '" + ingredient.toLowerCase() + "'";
	    		
	    		Cursor cursor = db.rawQuery(query, null);
	    		
	    		if (cursor.moveToFirst())
				{
					do
					{
						//Get drink information
						long id = cursor.getInt(0);
						String name = cursor.getString(1);
						Drink.Rating rating =  intToRating(cursor.getInt(2));
						String instructions = cursor.getString(3);
						
						//Get the ingredients for this specific drink
						ArrayList<Ingredient> drinkIngredients = getIngredientsForDrinkID(id);

						//Create drink and add to return list
						Drink currDrink = new Drink(name, rating, drinkIngredients, instructions, id);
						drinks.add(currDrink);
					} while (cursor.moveToNext());
				}
	    		
	    		cursor.close();
	    		
	    	}
	    	
	    	return drinks;
	    }
	    
	    /**
	     * Gets all ingredients for a given drink ID
	     * @param drinkID
	     * @param allMatches
	     * @param allPairs
	     * @return
	     */
	    private static ArrayList<Ingredient> getIngredientsForDrinkID(Long drinkID, ArrayList<Matching> allMatches, ArrayList<IngredientIDPair> allPairs)
		{
			ArrayList<Ingredient> ingList = new ArrayList<Ingredient>();
			for (Matching currMatch : allMatches)
			{
				if (currMatch.drinkID == drinkID)
				{
					Ingredient currIngredient = new Ingredient();
					currIngredient.setName(allPairs.get(currMatch.ingredientID-1).name);
					currIngredient.setQuantity(currMatch.quantity);
					currIngredient.setUnits(currMatch.units);
					ingList.add(currIngredient);
				}
				else if (currMatch.drinkID > drinkID)
				{
					break;
				}
			}
			return ingList;
		}
	    
	    /**
	     * Overloaded method that uses SQL instead of Java to create each ingredient
	     * given a drink id
	     * @param drinkID - ID of the drink
	     * @return - ArrayList<Ingredient> of all ingredients for specific drink
	     */
	    public ArrayList<Ingredient> getIngredientsForDrinkID(Long drinkID) {
	    	SQLiteDatabase db = this.getWritableDatabase();
	    	
	    	String query = "SELECT i.NAME, m.QUANTITY, m.UNITS "
   				 + "FROM Matchings m "
   				 + "JOIN Ingredients i on i.ID = m.INGREDIENTID "
   				 + "WHERE m.DRINKID = " + drinkID;
	    	
	    	ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
	    	
	    	Cursor cursor = db.rawQuery(query, null);
	    	
	    	//Create each ingredient from a row
	    	if (cursor.moveToFirst())
			{
				do
				{
					//Get all information for the ingredient
					String name = cursor.getString(0);
					String quantity = cursor.getString(1);
					String unit = cursor.getString(2);
					
					//Create ingredient and add it to the return list
					Ingredient ing = new Ingredient(name, quantity, unit);
					ingredients.add(ing);
					
				} while (cursor.moveToNext());
			}
	    	
	    	cursor.close();
	    	
	    	return ingredients;
	    }
	    
	    /**
	     * Converts an int to a rating
	     * @param i
	     * @return
	     */
	    private Rating intToRating(int i)
	    {
	    	if (i == 1)
	    		return Rating.THUMBSUP;
	    	else if (i == -1)
	    		return Rating.THUMBSDOWN;
	    	else
	    		return Rating.THUMBSNULL;
	    }
	    
	    /**
	     * Reads drinks from file
	     * @param filename path to the file
	     * @return A string containing the content of the file found at filename
	     */
	    private void readDrinks(SQLiteDatabase db)
	    {
	    	if (db == null)
	    	{
	    		db = this.getWritableDatabase();
	    	}
	    	List<Object> drinkData = new ArrayList<Object>();
	    	try {
	    		AssetManager assets = con.getResources().getAssets();
	    		InputStream is = assets.open(DRINK_FILE_NAME);
	    		BufferedReader br = null;

	    		try {
	    			br = new BufferedReader(new InputStreamReader(is));
	    			while (br.readLine() != null) 
	    			{
	    				int id = Integer.parseInt(br.readLine())+1;
	    				String name = br.readLine();
	    				int rating = stringToRatingToInt(br.readLine());
	    				String instructions = br.readLine();
	    				drinkData.add(id);
	    				drinkData.add(name);
	    				drinkData.add(rating);
	    				drinkData.add(instructions);
	    			}

	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (br != null) {
	    				try {
	    					addDrinkByVars(drinkData, db);
	    					br.close();
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    /**
	     * Reads matchings from file
	     * @param db this database
	     */
	    private void readMatchings(SQLiteDatabase db)
	    {
	    	if (db == null)
	    	{
	    		db = this.getWritableDatabase();
	    	}
	    	List<Matching> matchings = new ArrayList<Matching>();
	    	try {
	    		AssetManager assets = con.getResources().getAssets();
	    		InputStream is = assets.open(MATCH_FILE_NAME);
	    		BufferedReader br = null;
	    		
	    		try {
	    			br = new BufferedReader(new InputStreamReader(is));
	    			while (br.readLine() != null) {
	    				int id = Integer.parseInt(br.readLine())+1;
	    				int drinkID = Integer.parseInt(br.readLine())+1;
	    				int ingredientID = Integer.parseInt(br.readLine())+1;
	    				String quantity = br.readLine();
	    				if (quantity.equals("null"))
	    					quantity = "";
	    				String units = br.readLine();
	    				if (units.equals("null"))
	    					units = "";
	    				Matching match = new Matching(drinkID, ingredientID, id, quantity, units);
	    				matchings.add(match);
	    			}

	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (br != null) {
	    				try {
	    					addMatching(matchings, db);
	    					br.close();
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    /**
	     * Reads pairs from file
	     * @param db This database
	     */
	    private void readPairs(SQLiteDatabase db)
	    {
	    	if (db == null)
	    	{
	    		db = this.getWritableDatabase();
	    	}
	    	List<IngredientIDPair> pairs = new ArrayList<IngredientIDPair>();
	    	try {
	    		AssetManager assets = con.getResources().getAssets();
	    		InputStream is = assets.open(PAIR_FILE_NAME);
	    		BufferedReader br = null;
	    		try {

	    			br = new BufferedReader(new InputStreamReader(is));
	    			while (br.readLine() != null) {
	    				int id = Integer.parseInt(br.readLine())+1;
	    				String name = br.readLine();
	    				IngredientIDPair pair = new IngredientIDPair(id, name);
	    				pairs.add(pair);
	    			}

	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			if (br != null) {
	    				try {
	    					addPair(pairs, db);
	    					br.close();
	    				} catch (IOException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    /**
	     * Converts a string rating to an int
	     * @param str The rating in question
	     * @return -1 if thumbs down, 0 if thumbs null, +1 if thumbs up
	     */
	    private int stringToRatingToInt(String str)
	    {
	    	if (str.equals("THUMBSUP"))
	    	{
	    		return 1;
	    	}
	    	else if (str.equals("THUMBSDOWN"))
	    	{
	    		return -1;
	    	}
	    	else
	    	{
	    		return 0;
	    	}
	    }
}
