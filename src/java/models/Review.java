/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review data using Apache Lucene.
 */
package models;

/**
 * Model for a Review.
 * @author Lowell Milliken
 */
public class Review {
    private String asin;
    private String text;
    private String summary;
    private String reviewerId;
    private String reviewerName;
    private Book book;
    private int docId;
    private float score;

    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
    
}
