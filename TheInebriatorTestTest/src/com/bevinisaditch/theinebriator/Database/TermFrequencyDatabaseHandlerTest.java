package com.bevinisaditch.theinebriator.Database;

import java.util.ArrayList;

import com.bevinisaditch.theinebriator.ClassFiles.TermFrequency;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

public class TermFrequencyDatabaseHandlerTest extends AndroidTestCase {
	TermFrequencyDatabaseHandler handler;
	TermFrequency termFreq1;
	TermFrequency termFreq2;
	TermFrequency termFreq3;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		RenamingDelegatingContext context  = new RenamingDelegatingContext(getContext(), "test_");
		handler = new TermFrequencyDatabaseHandler(context);
		termFreq1 = new TermFrequency("test1", .1f);
		termFreq2 = new TermFrequency("test2", .3f);
		termFreq3 = new TermFrequency("test3", .6f);
	}

	public void testAddTermFreq() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		TermFrequency get1 = handler.getTermFrequency(id);
		assertEquals(termFreq1, get1);
	}
	
	public void testGetTermFreq_ByTerm() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		id = handler.addTermFreq(termFreq2);
		termFreq2.setID(id);
		
		TermFrequency get2 = handler.getTermFrequency(termFreq2.getTerm());
		assertEquals(termFreq2, get2);
		
		TermFrequency get1 = handler.getTermFrequency(termFreq1.getTerm());
		assertEquals(termFreq1, get1);
	}
	
	public void testGetTermFreq_Fail() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		TermFrequency get = handler.getTermFrequency(termFreq3.getTerm());
		assertEquals(null, get);
		
		TermFrequency get1 = handler.getTermFrequency(termFreq3.getID());
		assertEquals(null, get1);
	}
	
	public void testGetTermFreqCount() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		id = handler.addTermFreq(termFreq2);
		termFreq2.setID(id);
		
		Integer count = handler.getTermFreqCount();
		Integer expectedCount = 2;
		
		assertEquals(expectedCount, count);
		
	}
	
	public void testGetAllTermFreq() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		id = handler.addTermFreq(termFreq2);
		termFreq2.setID(id);
		
		id = handler.addTermFreq(termFreq3);
		termFreq3.setID(id);
		
		ArrayList<TermFrequency> allTermFreq = handler.getAllTermFreqs();
		
		assertEquals(3, handler.getTermFreqCount().intValue());
		assertTrue(allTermFreq.contains(termFreq1));
		assertTrue(allTermFreq.contains(termFreq2));
		assertTrue(allTermFreq.contains(termFreq3));
		
	}
	
	public void testUpdateTermFreq() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		id = handler.addTermFreq(termFreq2);
		termFreq2.setID(id);
		
		termFreq1.setTerm("TERMFREQ1");
		int count = handler.updateContact(termFreq1);
		
		assertEquals(1, count);
		assertEquals(termFreq1.getTerm(), "TERMFREQ1");
	}
	
	public void testUpdateTermFreq_Fail() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		termFreq2.setTerm("TERMFREQ1");
		int count = handler.updateContact(termFreq2);
		
		assertEquals(0, count);
	}
	
	public void testDeleteTermFreq() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		handler.deleteContact(termFreq1);
		
		assertEquals(0, handler.getTermFreqCount().intValue());
		
	}
	
	public void testDeleteTermFreq_Fail() {
		Long id = handler.addTermFreq(termFreq1);
		termFreq1.setID(id);
		
		handler.deleteContact(termFreq2);
		
		assertEquals(1, handler.getTermFreqCount().intValue());
		
	}

}