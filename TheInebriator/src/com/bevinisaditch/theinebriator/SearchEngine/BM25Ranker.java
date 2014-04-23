package com.bevinisaditch.theinebriator.SearchEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.bevinisaditch.theinebriator.Home;
import com.bevinisaditch.theinebriator.ClassFiles.Drink;
import com.bevinisaditch.theinebriator.ClassFiles.Ingredient;
import com.bevinisaditch.theinebriator.ClassFiles.TermFrequency;
import com.bevinisaditch.theinebriator.Database.TermFrequencyDatabaseHandler;

@SuppressLint("DefaultLocale")
public class BM25Ranker extends Ranker {
	public Context context;
	//public ProgressDialog loginDialog;
	ArrayList<Drink> relevantDrinks;
	ArrayList<String> searchTerms;
	TermFrequencyDatabaseHandler handler;
	Integer searchType;
	
	//Constants used in BM25
	private double b = .75;
	private double k = 1.2;
	
	public BM25Ranker(
			Context context,
			ArrayList<String> terms,
			ArrayList<Drink> drinks,
			Integer searchType) {
		this.context = context;
		//this.loginDialog = new ProgressDialog(context);
		this.relevantDrinks = drinks;
		this.searchTerms = terms;
		this.handler = new TermFrequencyDatabaseHandler(context);
		this.searchType = searchType;
		
	}
	
	
    protected void onPreExecute() {
    	super.onPreExecute();
    	/**
    	loginDialog.setMessage("Please wait... Searching");
    	loginDialog.setCancelable(false);
        loginDialog.show();
        **/  
    }
	
	@Override
	protected ArrayList<Drink> doInBackground(Void... params) {
		
		return rank(searchTerms, relevantDrinks);
	}
	
	@Override
    protected void onPostExecute(ArrayList<Drink> result) {
		if (context instanceof Home) {
			Home.backToListResults = false;
			Home.backToNoResults = true;
			((Home) context).listviewInit(result, true);
		}
		//loginDialog.dismiss();
    }
	

	@Override
	public ArrayList<Drink> rank(ArrayList<String> terms, ArrayList<Drink> drinks) {
		
		HashMap<Drink, Double> unsortedDrinks = new HashMap<Drink, Double>();
		
		ArrayList<String> individualTerms = parseTerms(terms);

		int averageLength = getAvgLengthOfDrinks(drinks);
		
		ArrayList<TermFrequency> termFreqs = new ArrayList<TermFrequency>();
		for (String term : individualTerms) {
			TermFrequency termFreq = handler.getTermFrequency(term.toLowerCase());
			if (termFreq != null) {
				termFreqs.add(termFreq);	
			} else {
				float freq = 0f;
				termFreqs.add(new TermFrequency(term, freq));
				Log.d("BM25Ranker", "No matching term freq for " + term);
				
			}
			
		}
		
		//For each drink, get a score
		for (Drink drink : drinks) {
			double score = 0.0;
			
			ArrayList<String> wordsInDrink = new ArrayList<String>();
			if (searchType == SearchEngine.SEARCH_NAME) {
				wordsInDrink.add(drink.getName());
			}
			
			if (searchType == SearchEngine.SEARCH_INGREDIENT) {
				for (Ingredient ing : drink.getIngredients()) {
					wordsInDrink.add(ing.getName());
				}
			}
			double totalFreq = parseTerms(wordsInDrink).size();
			//Log.d("BM25Ranker", "total frequency for " + drink.getName() + "is " + totalFreq);
			
			//Sum up all terms to get score
			for (TermFrequency termFreq : termFreqs) {
				
				String term = termFreq.getTerm();
				float freq = termFreq.getFrequency();
				
				double invDocFreq = Math.log((drinks.size()-freq+.5)/(freq + .5))/Math.log(2);
				//Log.d("BM25Ranker", "InvDocFreq for " + term + " is " + invDocFreq);
				
				
				double docFreq = 0.0;
				
				if (searchType == SearchEngine.SEARCH_NAME) {
					if (drink.getName().toLowerCase().contains(term.toLowerCase())) {
						docFreq += 1.0;
					}
				}
				
				if (searchType == SearchEngine.SEARCH_INGREDIENT) {
					for (Ingredient ingredient : drink.getIngredients()) {
						if ((ingredient.getName().toLowerCase()).contains(term.toLowerCase())) {
							docFreq += 1.0;
						}
						
					}
				}
				
				docFreq /= totalFreq;
				
				//Log.d("BM25Ranker", "docFreq for " + term + " in " + drink.getName() + " is " + docFreq);
				
				double numerator = docFreq*(k+1)*invDocFreq;
				double denominator = docFreq + k*(1-b+ b*(totalFreq/averageLength));
				
				score += numerator/denominator;
			}
			Log.d("BM25Ranker", "Score for " + drink.getName() + " is " + score);
			unsortedDrinks.put(drink, score);
		}
		
		ArrayList<Drink> sortedDrinks = sortDrinks(unsortedDrinks);
		
		return sortedDrinks;
	}
	
	/**
	 * Sort a hashmap of drinks based on their score
	 * @param unsortedDrinks - A Hashmap of Key=Drink, Value=Double (the score)
	 * @return sorted TreeMap of Drinks based on their score
	 */
	public ArrayList<Drink> sortDrinks(HashMap<Drink, Double> unsortedDrinks) {
		System.out.println("unsortedDrinks size: " + unsortedDrinks.size());
		DrinkComparator comparator = new DrinkComparator(unsortedDrinks);
		TreeMap<Drink, Double> sortedDrinks = new TreeMap<Drink, Double>(comparator);
		sortedDrinks.putAll(unsortedDrinks);
		System.out.println("SortedDrinks size: " + sortedDrinks.size());
		
		ArrayList<Drink> returnedDrinks = new ArrayList<Drink>();
		Entry<Drink, Double> currentEntry = sortedDrinks.pollFirstEntry();
		while (currentEntry != null) {
			Drink currentDrink = currentEntry.getKey();
			returnedDrinks.add(currentDrink);
			currentEntry = sortedDrinks.pollFirstEntry();
		}
		
		return returnedDrinks;
	}
	
	/**
	 * Takes in a list of search terms and parses them by all white space.
	 * For example, 'Orange Juice' would be come 'Orange' and 'Juice'
	 * 
	 * @param terms List of search terms
	 * @return list of terms parsed by white space
	 */
	public ArrayList<String> parseTerms(ArrayList<String> terms) {
		ArrayList<String> individualTerms = new ArrayList<String>();
		for (String term : terms) {
			String[] pieces = term.split("\\s+");
			individualTerms.addAll(Arrays.asList(pieces));
			
		}
		
		return individualTerms;
	}
	
	
	public int getAvgLengthOfDrinks(ArrayList<Drink> drinks) {
		int averageLength = 0;
		for (Drink drink : drinks){
			
			ArrayList<String> words = new ArrayList<String>();
			
			//Add name to length of document
			if (searchType == SearchEngine.SEARCH_NAME) {
				words.add(drink.getName());
				averageLength += parseTerms(words).size();
			}
			
			//Add ingredients to length of document
			if (searchType == SearchEngine.SEARCH_INGREDIENT) {
				for (Ingredient ing: drink.getIngredients()) {
					words.add(ing.getName());
				}
				
				averageLength += parseTerms(words).size();
			}
		}
		
		if (drinks.size() == 0) {
			return 0;
		}
		
		return averageLength/drinks.size();
	}
}

/**
 * Used for sorting drinks in a map based on their score.
 * 
 * @author michael
 */
class DrinkComparator implements Comparator<Drink> {
	Map<Drink, Double> drinks;
	
	public DrinkComparator(Map<Drink, Double> drinks) {
		this.drinks = drinks;
	}
	
	@Override
	public int compare(Drink a, Drink b) {
		if (drinks.get(a) >= drinks.get(b)) {
            return -1;
        } else {
            return 1;
        } 
		
	}	
	
}
