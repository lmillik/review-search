/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review and book data using Apache Lucene. Searches
 * can be run on the indexes using a simple boolean search with BM25 scoring using PRF or
 * Rocchio.
 *
 */
package searching;

import indexing.IndexConstants;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Book;
import models.Review;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Lowell Milliken
 */
public class QueryRunner {
    // Stores a query runner so that do not have to initialize twice.
    private static QueryRunner qRunner = null;
    
    // review searcher
    private IndexSearcher rSearcher;
    // book searcher
    private IndexSearcher bSearcher;
    // filtered review searcher
    private IndexSearcher filteredSearcher;

    /**
     * Returns a QueryRunner instance.
     * @return 
     */
    public static QueryRunner getQueryRunner() {
        if(qRunner == null) {
            qRunner = new QueryRunner(IndexConstants.INDEX_LOCATION, IndexConstants.BOOK_INDEX_LOCATION, IndexConstants.FILTERED_INDEX_LOCATION);
        }
        
        return qRunner;
    }
    
    /**
     * 
     * @param indexPath path to review index
     * @param bookIndexPath path to book index
     */
    private QueryRunner(String indexPath, String bookIndexPath) {
        try {
            System.out.println("Loading index...");
            File indexFile = new File(indexPath);
            Directory dir = FSDirectory.open(indexFile.toPath());
            IndexReader reader = DirectoryReader.open(dir);
            rSearcher = new IndexSearcher(reader);
            rSearcher.setSimilarity(new BM25Similarity());
            System.out.println(rSearcher.collectionStatistics(IndexConstants.ASIN).docCount());

            File bindexFile = new File(bookIndexPath);
            Directory bdir = FSDirectory.open(bindexFile.toPath());
            IndexReader breader = DirectoryReader.open(bdir);
            bSearcher = new IndexSearcher(breader);
            bSearcher.setSimilarity(new BM25Similarity());
            System.out.println(bSearcher.collectionStatistics(IndexConstants.ASIN).docCount());

        } catch (IOException ex) {
            System.err.println("Error opening index: " + ex.getMessage());
        }
    }

    /**
     * 
     * @param indexPath path to review index
     * @param bookIndexPath path to book index
     * @param filteredIndexPath path to filtered review index
     */
    private QueryRunner(String indexPath, String bookIndexPath, String filteredIndexPath) {
        this(indexPath, bookIndexPath);
        try {
            File indexFile = new File(filteredIndexPath);
            Directory dir = FSDirectory.open(indexFile.toPath());
            IndexReader reader = DirectoryReader.open(dir);
            filteredSearcher = new IndexSearcher(reader);
            filteredSearcher.setSimilarity(new BM25Similarity());
            System.out.println(filteredSearcher.collectionStatistics(IndexConstants.ASIN).docCount());
        } catch (IOException ex) {
            System.err.println("Error opening index: " + ex.getMessage());
        }
    }

    /**
     * Execute a simple search through reviews
     * @param text query text
     * @param field query field
     * @param n number of results to return
     * @param and true = use AND, false = use OR
     * @param searcher searcher to use
     * @return list of reviews found in order
     */
    private List<Review> executeReview(String text, String field, int n, boolean and, IndexSearcher searcher) {
        List<Review> results = new ArrayList<>();

        try {
            TopDocs topDocs = execute(text, field, searcher, n, and);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                results.add(docToReview(doc, scoreDoc));
            }

        } catch (IOException ex) {
            System.err.println("IO Error while searching: " + ex.getMessage());
        }

        return results;
    }

    /**
     * Execute a simple search on the unfiltered reviews.
     * @param text query text
     * @param field query field
     * @param n number of results to return
     * @param and true = use AND, false = use OR
     * @return list of reviews found in order
     */
    public List<Review> executeReview(String text, String field, int n, boolean and) {
        return executeReview(text, field, n, and, rSearcher);
    }

    /**
     * Execute a simple search on the filtered reviews.
     * @param text query text
     * @param field query field
     * @param n number of results to return
     * @param and true = use AND, false = use OR
     * @return list of reviews found in order
     */
    public List<Review> executeReviewFiltered(String text, String field, int n, boolean and) {
        return executeReview(text, field, n, and, filteredSearcher);
    }

    /**
     * Convert a Lucene Document to a Review
     * @param doc Document to convert
     * @param scoreDoc scoreDoc of the document with id and score
     * @return converted Review
     */
    public static Review docToReview(Document doc, ScoreDoc scoreDoc) {
        Review review = new Review();
        IndexableField field = doc.getField(IndexConstants.ASIN);
        review.setAsin((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.REVIEWER_ID);
        review.setReviewerId((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.REVIEWER_NAME);
        review.setReviewerName((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.SUMMARY);
        review.setSummary((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.TEXT);
        review.setText((field != null) ? field.stringValue().trim() : "N/A");
        review.setDocId(scoreDoc.doc);
        review.setScore(scoreDoc.score);
        return review;
    }

    /**
     * Execute a simple search on the books.
     * @param text query text
     * @param field query field
     * @param n number of results to return
     * @param and true = use AND, false = use OR
     * @return list of reviews found in order
     */
    public List<Book> executeBook(String text, String field, int n, boolean and) {
        List<Book> results = new ArrayList<>();

        try {
            TopDocs topDocs = execute(text, field, bSearcher, n, and);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = bSearcher.doc(scoreDoc.doc);
                results.add(docToBook(doc));
            }

        } catch (IOException ex) {
            System.err.println("IO Error while searching: " + ex.getMessage());
        }

        return results;
    }

    /**
     * Checks the book index for a ASIN.
     * @param text ASIN to check
     * @return true: the ASIN is in the index
     */
    public boolean checkASIN(String text) {
        try {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            TermQuery tq = new TermQuery(new Term(IndexConstants.ASIN, text.trim()));
            builder.add(tq, BooleanClause.Occur.MUST);
            TopDocs topDocs = bSearcher.search(builder.build(), 1);
            if (topDocs.totalHits > 0) {
                return true;
            }
        } catch (IOException ex) {
            System.err.println("IO Error while searching: " + ex.getMessage());
        }

        return false;
    }

    /**
     * Checks the book index for a book with a ASIN and subject.
     * @param asin ASIN to check in
     * @param text subject to check
     * @return true if an ASIN with the subject exists in the index
     */
    public boolean checkSubject(String asin, String text) {
        try {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            TermQuery tq = new TermQuery(new Term(IndexConstants.ASIN, asin.trim()));
            builder.add(tq, BooleanClause.Occur.MUST);
            builder.add(buildQuery(text, IndexConstants.SUBJECT, false), BooleanClause.Occur.MUST);
            TopDocs topDocs = bSearcher.search(builder.build(), 1);
            if (topDocs.totalHits > 0) {
                return true;
            }
        } catch (IOException ex) {
            System.err.println("IO Error while searching: " + ex.getMessage());
        }

        return false;
    }

    /**
     * Convert a Lucene Document to a Book.
     * @param doc Document to convert
     * @return converted Book
     */
    public static Book docToBook(Document doc) {
        Book book = new Book();
        IndexableField field = doc.getField(IndexConstants.ASIN);
        book.setAsin((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.AUTHOR);
        book.setAuthors((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.SUBJECT);
        book.setSubjects((field != null) ? field.stringValue().trim() : "N/A");
        field = doc.getField(IndexConstants.TITLE);
        book.setTitle((field != null) ? field.stringValue().trim() : "N/A");
        return book;
    }

    /**
     * Perform a PRF search.
     * @param text query text
     * @param field query field
     * @param n number of results to return
     * @param x number of terms to add
     * @param filtered true: search in filtered reviews, false: search in unfiltered reviews
     * @return 
     */
    public List<Review> prf(String text, String field, int n, int x, boolean filtered) {
        RocchioConfig config = new RocchioConfig();
        config.setX(x);
        return rocchio(text, field, n, config, filtered);
    }

    /**
     * Run a Rocchio search. If x > ) in config, run a PRF search. This method uses
     * Lucene boosts as term weights for Rocchio.
     * @param text Free text query.
     * @param field field to run query on
     * @param n number of results to return
     * @param config rocchio configuration
     * @param filtered true: use filtered review set. false: use unfiltered
     * review set.
     * @param fromBooks true: use book subject search and reviews of those books for expansion
     * @return List of top reviews
     */
    public List<Review> rocchio(String text, String field, int n, RocchioConfig config, boolean filtered, boolean fromBooks) {
        int x = config.getX();
        int r = config.getR();
        float alpha = config.getAlpha();
        float beta = config.getBeta();

        // intermediate results
        List<Review> results = new ArrayList<>();
        IndexSearcher searcher = filtered ? filteredSearcher : rSearcher;

        if (fromBooks) {
            List<Book> books;
            
            // book subject search
            books = executeBook(text, IndexConstants.SUBJECT, r, false);
            for (Book book : books) {
                try {
                    // run review search for top review for this book
                    TermQuery tq = new TermQuery(new Term(IndexConstants.ASIN, book.getAsin().trim()));
                    TopDocs topDocs = searcher.search(tq, 1);
                
                    // if the review exists (which it should) add it to the results
                    if (topDocs.totalHits > 0) {
                        results.add(docToReview(searcher.doc(topDocs.scoreDocs[0].doc), topDocs.scoreDocs[0]));
                    }
                } catch (IOException ex) {
                    System.err.println("IO Error while searching: " + ex.getMessage());
                }
            }
        } else {
            // run a simple search for reviews
            results = executeReview(text, field, r, false, searcher);
        }
        
        // term freqencies in the query text
        Map<String, Integer> termCounts = new HashMap<>();
        
        // must tokenize, stem, and de-stop word the query while counting occurances
        Analyzer analyzer = new EnglishAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(field, text);
        CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                if(!termCounts.containsKey(termAtt.toString())) {
                    termCounts.put(termAtt.toString(), 1);
                } else {
                    termCounts.put(termAtt.toString(), termCounts.get(termAtt.toString()) + 1);
                }
            }
            tokenStream.close();
        } catch (IOException ex) {
            System.err.println("IO Error while reading query text: " + ex.getMessage());
        }

        // build the original query
        BooleanQuery origQuery = buildQuery(text, field, false);
        // extract the boolean clauses
        List<BooleanClause> clauses = origQuery.clauses();
        // list of queries to be built into final query
        List<TermQuery> queries = new ArrayList<>();
        // boost factor for each term
        Map<String, Float> boosts = new HashMap<>();

        // total number of documents
        int numDocs = searcher.getIndexReader().numDocs();
        // for each clause: one term per clause
        for (BooleanClause clause : clauses) {
            // store the term queries for later
            TermQuery termQuery = (TermQuery) clause.getQuery();
            queries.add(termQuery);
            // if the boost factor for this term does not already exist
            if(!boosts.containsKey(termQuery.getTerm().text())) {
                int docFreq;
                try {
                    // get document frequency for the term
                    docFreq = searcher.getIndexReader().docFreq(termQuery.getTerm());
                    // put the tf-idf score as the boost
                    boosts.put(termQuery.getTerm().text(), (float)calcTFIDF(numDocs, docFreq, termCounts.get(termQuery.getTerm().text())));
                } catch (IOException ex) {
                    System.err.println("Error getting doc freq: " + ex.getMessage());
                }
            }
        }

        // scores for each term
        List<TermScore> termScores = new ArrayList<>();
        // boosts to add to original boosts after going though Rocchio
        Map<String, Float> addBoosts = new HashMap<>();
        // for each intermediate result
        for (Review result : results) {
            try {
                termScores.clear();
                // get the term vector for this document
                Terms terms = searcher.getIndexReader().getTermVector(result.getDocId(), field);
                // TermsEnum can iterate through the term vector
                TermsEnum termsEnum = terms.iterator();
                while (termsEnum.next() != null) {
                    // get the term string
                    String term = termsEnum.term().utf8ToString();
                    // get the postings for this term
                    // since we got the termEnum from a document, the only document is that one
                    PostingsEnum pe = null;
                    pe = termsEnum.postings(pe);
                    pe.nextDoc();

                    // calculate the tf-idf score for this term/document
                    double score = calcTFIDF(numDocs, termsEnum.docFreq(), pe.freq());

                    termScores.add(new TermScore(term, score));
                }

                // if PRF sort the terms
                if (x > 0) {
                    Collections.sort(termScores);
                    termScores = termScores.subList(0, x);
                }

                // for each term
                for (TermScore termScore : termScores) {
                    String term = termScore.term;
                    
                    // if Rocchio
                    if (x <= 0) {
                        // add the tf-idf score for the term divided by the number of relevant documents
                        // to the boosts to be added, also add the term to the queries
                        if (addBoosts.containsKey(term)) {
                            addBoosts.put(term, addBoosts.get(term) + (float) termScore.score / results.size());
                        } else {
                            addBoosts.put(term,(float) termScore.score / results.size());
                            queries.add(new TermQuery(new Term(field, term)));
                        }
                    } else if (!boosts.containsKey(term)) { // else PRF if the term is not in the original query
                        boosts.put(term, 1.0f);
                        queries.add(new TermQuery(new Term(field, term)));
                    }
                }
            } catch (IOException ex) {
                System.err.println("IO Error while retrieving term vector: " + ex.getMessage());
            }
        }
        
        // builder to create the boolean query
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // counting sub queries in the boolean query
        // the max is 1024, playing it safe here with 1000
        // for many long reviews in the intermediate results
        // it is possible to hit this limit
        int count = 0;
        // for each sub query
        for (TermQuery query : queries) {
            // get the term string
            String term = query.getTerm().text();
            
            // for Rocchio, add the new boosts in.
            // These boosts function as the term weights in Rocchio.
            if(x<=0) {
                if(boosts.containsKey(term) && addBoosts.containsKey(term)) {
                    boosts.put(term, alpha*boosts.get(term) + beta*addBoosts.get(term));
                } else if (addBoosts.containsKey(term)) {
                    boosts.put(term, beta*addBoosts.get(term));
                }
            }
            
            // If Rocchio use the boosts. If PRF set all boosts to 1
            BoostQuery boostQuery = new BoostQuery(query, (x<=0) ? boosts.get(term) : 1.0f);
            builder.add(boostQuery, BooleanClause.Occur.SHOULD);
            count++;
            if(count>=1000) {
                break;
            }
        }

        // run the query and get the results
        List<Review> newResults = new ArrayList<>();
        try {
            TopDocs topDocs = searcher.search(builder.build(), n);
            if (topDocs != null) {
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    newResults.add(docToReview(doc, scoreDoc));
                }
            }
        } catch (IOException ex) {
            System.err.println("IO Error while searching: " + ex.getMessage());
        }
        return newResults;
    }

    /**
     * Run a Rocchio search. If x > ) in config, run a PRF search. This method uses
     * Lucene boosts as term weights for Rocchio. This version does not use books
     * to find intermediate reviews.
     * @param text Free text query.
     * @param field field to run query on
     * @param n number of results to return
     * @param config rocchio configuration
     * @param filtered true: use filtered review set. false: use unfiltered
     * review set.
     * @return List of top reviews
     */
    public List<Review> rocchio(String text, String field, int n, RocchioConfig config, boolean filtered) {
        return rocchio(text, field, n, config, filtered, false);
    }

    /**
     * Calculates tf-idf.
     * @param numDocs total number of documents in index
     * @param docFreq document frequency of the term
     * @param termFreq term frequency in document
     * @return 
     */
    private double calcTFIDF(int numDocs, int docFreq, int termFreq) {
        double idf = Math.log10((double) numDocs / docFreq);
        double tf = 1 + Math.log10((double) termFreq);
        return tf * idf;
    }

    /**
     * Builder a BooleanQuery from the query text, for the  query field.
     * @param text query text
     * @param field query field
     * @param and true: use AND, false: use OR
     * @return 
     */
    private BooleanQuery buildQuery(String text, String field, boolean and) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Analyzer analyzer = new EnglishAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(field, text);
        CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                Query termQuery = new TermQuery(new Term(field, term.toString()));
                builder.add(termQuery, and ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD);
            }
            tokenStream.close();
        } catch (IOException ex) {
            System.err.println("IO Error while reading query text: " + ex.getMessage());
        }

        return builder.build();
    }

    /**
     * Builds a query and executes on the given searcher.
     * @param text query text
     * @param field query field
     * @param iSearcher searcher to use
     * @param n number of results to return
     * @param and true: use AND, false: use OR
     * @return TopDocs object containing the results
     * @throws IOException 
     */
    private TopDocs execute(String text, String field, IndexSearcher iSearcher, int n, boolean and) throws IOException {
        BooleanQuery booleanQuery = buildQuery(text, field, and);
        return iSearcher.search(booleanQuery, n);
    }

    /**
     * Returns the book searcher.
     * @return book index searcher.
     */
    public IndexSearcher getbSearcher() {
        return bSearcher;
    }

    /**
     * This class holds terms and term scores so they can be sorted.
     */
    private static class TermScore implements Comparable {

        private final String term;
        private final double score;

        public TermScore(String term, double score) {
            this.term = term;
            this.score = score;
        }

        public String getTerm() {
            return term;
        }

        public double getScore() {
            return score;
        }

        @Override
        public int compareTo(Object o) {
            TermScore otherScore = (TermScore) o;
            if (score < otherScore.getScore()) {
                return 1;
            } else if (score > otherScore.getScore()) {
                return -1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return term + ": " + score;
        }

    }

    /**
     * This class holds the Rocchio algorithm parameters.
     */
    public static class RocchioConfig {

        private int x;
        private int r;
        private float alpha;
        private float beta;

        /**
         * Constructor with default parameters.
         * x = -1
         * r = 10
         * alpha = 1
         * beta = 0.8
         */
        public RocchioConfig() {
            x = -1;
            r = 10;
            alpha = 1f;
            beta = .8f;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getR() {
            return r;
        }

        public void setR(int r) {
            this.r = r;
        }

        public float getAlpha() {
            return alpha;
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        public float getBeta() {
            return beta;
        }

        public void setBeta(float beta) {
            this.beta = beta;
        }

    }
}
