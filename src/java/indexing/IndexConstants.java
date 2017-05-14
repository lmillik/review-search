/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review data using Apache Lucene. Your must
 * change the INDEX_LOCATION constants to valid locations on your system.
 */
package indexing;

/**
 *
 * Contains indexing related constants.
 * 
 * @author Lowell Milliken
 */
public class IndexConstants {
    public final static String TEXT = "reviewText";
    public final static String ASIN = "asin";
    public final static String UNIX_TIME = "unixReviewTime";
    public final static String RATING = "overall";
    public final static String REVIEWER_ID = "reviewerID";
    public final static String REVIEWER_NAME = "reviewerName";
    public final static String SUMMARY = "summary";
    
    public final static String AUTHOR = "author";
    public final static String TITLE = "title";
    public final static String SUBJECT = "subject";
    public final static String PUBLISHER = "publisher";
    
    // THIS SHOULD BE A VALID LOCATION ON YOUR SYSTEM
    public final static String INDEX_LOCATION = "C:\\Users\\LowellStandard\\Documents\\849\\Term Project\\SearchEngine 4\\index";
    public final static String BOOK_INDEX_LOCATION = "C:\\Users\\LowellStandard\\Documents\\849\\Term Project\\SearchEngine 4\\bookIndex";
    public final static String FILTERED_INDEX_LOCATION = "C:\\Users\\LowellStandard\\Documents\\849\\Term Project\\SearchEngine 4\\filteredIndex";
}
