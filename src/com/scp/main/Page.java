package com.scp.main;

import java.util.ArrayList;
import java.util.Arrays;

public class Page {
	
	
	private String pageLink;
	private String title;
	private Integer rating;
	private String createdBy;
	private java.sql.Timestamp createdAt;
	
	private Boolean scpPage = false;
	private String scpTitle;

    private ArrayList<String> tags = new ArrayList<>();

	/**
	 * 
	 * @param pageLink scp wikidot name the-best-laid-plans-of-mice-and-men
	 * @param title The display title.  The Best Laid Plans of Mice And Men
	 * @param rating Integer rating
	 * @param createdBy Who wrote it
	 * @param createdAt When it was created
	 * @param scpPage is this an scp article?
	 * @param scpTitle If above is true, this is the title from the series page
	 */
	public Page (String pageLink, String title, Integer rating, String createdBy,
			java.sql.Timestamp createdAt, Boolean scpPage, String scpTitle, ArrayList<String> tags){
		this.pageLink = pageLink;
		this.title = title;
		this.rating = rating;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.scpPage = scpPage;
		this.scpTitle = scpTitle;
		this.tags = tags;
	}

	public Page (String pageLink){
		this.pageLink = pageLink;
	}
	
	public Page (String pageLink, String title,Boolean scpPage, String scpTitle){
		this.pageLink = pageLink;
		this.title = title;
		this.scpPage = scpPage;
		this.scpTitle = scpTitle;
	}
	
	public Page (String pageLink, String title, Integer rating, String createdBy,
			java.sql.Timestamp createdAt, Boolean scpPage, String scpTitle){
		this.pageLink = pageLink;
		this.title = title;
		this.rating = rating;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.scpPage = scpPage;
		this.scpTitle = scpTitle;
	}
	
	
	public boolean searchTest(String[] terms){
		String search = null;
		if(scpPage){
			search = scpTitle;
		}else{
			search = title;
		}
		
		String strLow = search.toLowerCase();
		ArrayList<String> words = new ArrayList<String>();
		words.addAll(Arrays.asList(strLow.split(" ")));
		for(int i = 1; i < terms.length; i++){
			if(!words.contains(terms[i].toLowerCase())){
				return false;
			}
		}
		return true;
	}


	public String getPageLink() {
		return pageLink;
	}


	public String getTitle() {
		return title;
	}


	public Integer getRating() {
		return rating;
	}


	public String getCreatedBy() {
		return createdBy;
	}


	public java.sql.Timestamp getCreatedAt() {
		return createdAt;
	}


	public Boolean getScpPage() {
		return scpPage;
	}


	public String getScpTitle() {
		return scpTitle;
	}
	
	public void setTags(ArrayList<String> tags){
		this.tags = tags;
	}
	
	public void setScpTitle(String title){
		this.scpTitle = title;
	}
	
	public void setScpPage(Boolean page){
		this.scpPage = page;
	}
	
	public ArrayList<String> getTags(){
		return tags;
	}

	@Override
	public boolean equals(Object otherPage){
			return otherPage instanceof Page && ((Page)otherPage).getPageLink().equals(this.getPageLink());
	}
	
	
}
