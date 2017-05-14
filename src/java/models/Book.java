/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review data using Apache Lucene.
 */
package models;

/**
 * Model for a Book.
 * @author Lowell Milliken
 */
public class Book {
    private String title;
    private String authors;
    private String subjects;
    private String asin;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }
    
}
