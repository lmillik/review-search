/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review and book data using Apache Lucene. Searches
 * can be run on the indexes using a simple boolean search with BM25 scoring using PRF or
 * Rocchio.
 *
 */
package ui;

import indexing.IndexConstants;
import indexing.IndexGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import models.Book;
import models.Review;
import searching.QueryRunner;

/**
 * Command line UI for using the index generator and the query
 * evaluation.
 *
 * @author Lowell Milliken
 */
public class CommandLineUI {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String text;
        List<Review> results;
        QueryRunner.RocchioConfig rConfig;
        while (true) {
            char choice = menu();

            switch (choice) {
                case 'a':
                    createIndex(false, "review_data_part_1.json");
                    return;
                case 'b':
                    createBookIndex();
                    return;
                case 'c':
                    createIndex(true, "review_data_part_1.json");
                    return;
                case 'd':
                    System.out.println("Enter a search term.");
                    text = in.nextLine();
                    results = QueryRunner.getQueryRunner().executeReview(text, IndexConstants.TEXT, 10, false);
                    showResults(results);
                    break;
                case 'e':
                    System.out.println("Enter a search term.");
                    text = in.nextLine();
                    rConfig = new QueryRunner.RocchioConfig();
                    rConfig.setX(3);
                    rConfig.setR(1);
                    results = QueryRunner.getQueryRunner().rocchio(text, IndexConstants.TEXT, 10, rConfig, false);
                    showResults(results);
                    break;
                case 'f':
                    System.out.println("Enter a search term.");
                    text = in.nextLine();
                    rConfig = new QueryRunner.RocchioConfig();
                    results = QueryRunner.getQueryRunner().rocchio(text, IndexConstants.TEXT, 10, rConfig, false, true);
                    showResults(results);
                    break;
                case 'g':
                    return;
                case 'h':
                    System.out.println("Enter a search term.");
                    text = in.nextLine();

                    List<Book> books = QueryRunner.getQueryRunner().executeBook(text, IndexConstants.SUBJECT, 10, false);
                    for (Book book : books) {
                        System.out.println(book.getAsin());
                        System.out.println(book.getAuthors());
                        System.out.println(book.getTitle());
                        System.out.println(book.getSubjects());
                    }
                    break;
                case 'i':
                    doTests(QueryRunner.getQueryRunner());
                    return;
                case 'j':
                    for (int i = 0; i < 50; i++) {
                        createIndex(false, "review_data_part_" + i + ".json");
                        createIndex(true, "review_data_part_" + i + ".json");
                    }
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    /**
     * Displays the command line menu
     *
     * @return returns the char representing the users choice from the menu
     */
    private static char menu() {
        System.out.println("-------------------------------------------");
        System.out.println("a) Create an index file."
                + " The program will exit when this in finished.");
        System.out.println("b) Create an index for the books file.");
        System.out.println("c) Create a filtered index.");
        System.out.println("d) Perform a search using the index file (basic).");
        System.out.println("e) Perform a search using the index file (PRF).");
        System.out.println("f) Perform a search using the index file (Rocchio).");
        System.out.println("g) Quit");
        System.out.println("h) Perform a subject book search.");
        System.out.println("i) Perform many searches for testing (very time consuming).");
        System.out.println("j) Index 50 review data partitions.");
        System.out.print("Enter your choice: ");

        Scanner input = new Scanner(System.in);
        String inputLine = input.nextLine();

        return inputLine.charAt(0);
    }

    /**
     * Creates an index file from a review json file.
     * 
     * @param filtered filter by books or not
     * @param sourcePath path to the documents file
     */
    private static void createIndex(boolean filtered, String sourcePath) {
        System.out.println("Generating Index");
        File documentsFile = new File(sourcePath);
        IndexGenerator generator = new IndexGenerator(documentsFile);
        File indexFile = new File(filtered ? IndexConstants.FILTERED_INDEX_LOCATION : IndexConstants.INDEX_LOCATION);
        generator.createIndex(indexFile, filtered);
        System.out.println("Done.");
    }

    /**
     * Creates an index file from a book metadata file.
     */
    private static void createBookIndex() {
        System.out.println("Generating Book Index");
        File books = new File("book_data.json");
        IndexGenerator generator = new IndexGenerator(books);
        File indexFile = new File(IndexConstants.BOOK_INDEX_LOCATION);
        generator.createBookIndex(books, indexFile);
        System.out.println("Done.");
    }

    /**
     * Displays review information.
     * @param results list of reviews
     */
    private static void showResults(List<Review> results) {
        for (Review result : results) {
            System.out.println("Score: " + result.getScore());
            System.out.println("ASIN: " + result.getAsin());
            System.out.println("Summary: " + result.getSummary());
            System.out.println("Review Text: " + result.getText());
            System.out.println("--");
        }
    }

    /**
     * Perform many searches and output to files in trec_eval input format.
     * 
     * @param qRunner QueryRunner to use while testing
     */
    private static void doTests(QueryRunner qRunner) {
        
        // parameter bounds and steps
        int xmin = 1;
        int xmax = 10;
        int xstep = 1;

        int rmin = 1;
        int rmax = 10;
        int rstep = 1;

        float alphamin = .5f;
        float alphamax = 1.2f;
        float alphastep = .1f;

        float betamin = .5f;
        float betamax = 1.2f;
        float betastep = .1f;

        int labeledResultN = 100;
        int unlabeledResultN = 10;

        // queries for the filtered reviews
        String[] labeledQueries = {"science fiction", "mystery", "cooking", "politics", "romance"};
        // queries for the unfiltered reviews
        String[] unlabeledQueries = {"feel good", "noir", "grim dark", "artificial intelligence", "health care"};
        String field = IndexConstants.TEXT;
        
        // directories to save results in
        String prfLDir = "prf results labeled";
        String rocLDir = "rocchio results labeled";
        String prfDir = "prf results";
        String rocDir = "rocchio results";

        // simple searches for the labeled queries
        for (int queryNo = 1; queryNo <= labeledQueries.length; queryNo++) {
            String query = labeledQueries[queryNo - 1];
            List<Review> results = qRunner.executeReviewFiltered(query, field, labeledResultN, false);
            String filename = "simpleresults labeled.txt";
            ResultsWriter writer = new ResultsWriter(filename, queryNo, query, true);
            writer.toFile(results, qRunner);
            writer.writeQrels();
        }

        // check or make the directory
        new File(prfLDir).mkdir();

        // run PRF searches
        for (int x = xmin; x <= xmax; x += xstep) {
            for (int queryNo = 1; queryNo <= labeledQueries.length; queryNo++) {
                String query = labeledQueries[queryNo - 1];
                List<Review> results = qRunner.prf(query, field, labeledResultN, x, true);
                String filename = prfLDir + "/" + x + ".txt";
                ResultsWriter writer = new ResultsWriter(filename, queryNo, query, true);
                writer.toFile(results, qRunner);
                writer.writeQrels();
            }
        }
        // check or make the directory
        new File(rocLDir).mkdir();
        
        // run Rocchio searches
        for (int r = rmin; r <= rmax; r += rstep) {
            for (float alpha = alphamin; alpha <= alphamax; alpha += alphastep) {
                for (float beta = betamin; beta <= betamax; beta += betastep) {
                    for (int queryNo = 1; queryNo <= labeledQueries.length; queryNo++) {
                        String query = labeledQueries[queryNo - 1];
                        QueryRunner.RocchioConfig rConfig = new QueryRunner.RocchioConfig();
                        // x = -1 means do Rocchio search
                        rConfig.setX(-1);
                        rConfig.setR(r);
                        rConfig.setAlpha(alpha);
                        rConfig.setBeta(beta);

                        List<Review> results = qRunner.rocchio(query, field, labeledResultN, rConfig, true);
                        String filename = rocLDir + "/" + r + " " + alpha + " " + beta + ".txt";
                        ResultsWriter writer = new ResultsWriter(filename, queryNo, query, true);
                        writer.toFile(results, qRunner);
                        writer.writeQrels();
                    }
                }
            }
        }
        
        // run Rocchio search with book subject based initial search
        for (int r = rmin; r <= rmax; r += rstep) {
            for (float alpha = alphamin; alpha <= alphamax; alpha += alphastep) {
                for (float beta = betamin; beta <= betamax; beta += betastep) {
                    for (int queryNo = 1; queryNo <= labeledQueries.length; queryNo++) {
                        String query = labeledQueries[queryNo - 1];
                        QueryRunner.RocchioConfig rConfig = new QueryRunner.RocchioConfig();
                        // x = -1 means do Rocchio search
                        rConfig.setX(-1);
                        rConfig.setR(r);
                        rConfig.setAlpha(alpha);
                        rConfig.setBeta(beta);

                        List<Review> results = qRunner.rocchio(query, field, labeledResultN, rConfig, true, true);
                        String filename = rocLDir + "/" + r + " " + alpha + " " + beta + " books.txt";
                        ResultsWriter writer = new ResultsWriter(filename, queryNo, query, true);
                        writer.toFile(results, qRunner);
                        writer.writeQrels();
                    }
                }
            }
        }

        // run simple search queries on unlabeled reviews
        for (int queryNo = 1; queryNo <= unlabeledQueries.length; queryNo++) {
            String query = unlabeledQueries[queryNo - 1];
            List<Review> results = qRunner.executeReview(query, field, unlabeledResultN, false);
            String filename = "simple results.txt";
            ResultsWriter writer = new ResultsWriter(filename, queryNo, query, false);
            writer.toFile(results, qRunner);
        }
        // check or make the directory
        new File(prfDir).mkdir();

        // run PRF searches
        for (int x = xmin; x <= xmax; x += xstep) {
            for (int queryNo = 1; queryNo <= unlabeledQueries.length; queryNo++) {
                String query = unlabeledQueries[queryNo - 1];
                List<Review> results = qRunner.prf(query, field, unlabeledResultN, x, false);
                String filename = prfDir + "/"+x+".txt";
                ResultsWriter writer = new ResultsWriter(filename, queryNo, query, false);
                writer.toFile(results, qRunner);
            }
        }
        // check or make the directory
        new File(rocDir).mkdir();
        
        // run Rocchio searches
        for (int r = rmin; r <= rmax; r += rstep) {
            for (float alpha = alphamin; alpha <= alphamax; alpha += alphastep) {
                for (float beta = betamin; beta <= betamax; beta += betastep) {
                    for (int queryNo = 1; queryNo <= unlabeledQueries.length; queryNo++) {
                        String query = unlabeledQueries[queryNo - 1];
                        QueryRunner.RocchioConfig rConfig = new QueryRunner.RocchioConfig();
                        
                        // x = -1 means do Rocchio search
                        rConfig.setX(-1);
                        rConfig.setR(r);
                        rConfig.setAlpha(alpha);
                        rConfig.setBeta(beta);

                        List<Review> results = qRunner.rocchio(query, field, unlabeledResultN, rConfig, false);
                        String filename = rocDir + "/"+ r + " " + alpha + " " + beta + ".txt";
                        ResultsWriter writer = new ResultsWriter(filename, queryNo, query, false);
                        writer.toFile(results, qRunner);
                    }
                }
            }
        }
        
        // run Rocchio searches with book subject based initial search
        for (int r = rmin; r <= rmax; r += rstep) {
            for (float alpha = alphamin; alpha <= alphamax; alpha += alphastep) {
                for (float beta = betamin; beta <= betamax; beta += betastep) {
                    for (int queryNo = 1; queryNo <= unlabeledQueries.length; queryNo++) {
                        String query = unlabeledQueries[queryNo - 1];
                        QueryRunner.RocchioConfig rConfig = new QueryRunner.RocchioConfig();
                        // x = -1 means do Rocchio search
                        rConfig.setX(-1);
                        rConfig.setR(r);
                        rConfig.setAlpha(alpha);
                        rConfig.setBeta(beta);

                        List<Review> results = qRunner.rocchio(query, field, unlabeledResultN, rConfig, false, true);
                        String filename = rocDir + "/"+ r + " " + alpha + " " + beta + " books.txt";
                        ResultsWriter writer = new ResultsWriter(filename, queryNo, query, false);
                        writer.toFile(results, qRunner);
                    }
                }
            }
        }
    }

    /**
     * Writes results to file in trev_eval input format.
     */
    private static class ResultsWriter {

        private final String filename;
        private final String query;
        private final int queryNo;
        private final Set<String> relevant;
        private final Set<String> notRelevant;
        private final boolean filtered;

        /**
         * 
         * @param filename output file name
         * @param queryNo query number
         * @param query query text
         * @param filtered filtered reviews or not
         */
        public ResultsWriter(String filename, int queryNo, String query, boolean filtered) {
            this.filename = filename;
            this.queryNo = queryNo;
            this.query = query;
            this.filtered = filtered;

            relevant = new HashSet<>();
            notRelevant = new HashSet<>();
        }

        /**
         * Writes the results list to file. If the filtered flag is true, store qrels
         * based on book index searches.
         * @param results a list of review results
         * @param qRunner QueryRunner to run book searches on
         */
        public void toFile(List<Review> results, QueryRunner qRunner) {
            File file = new File(filename);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                int rank = 1;
                for (Review result : results) {
                    writer.write(queryNo + " 0 ");
                    writer.write(result.getAsin() + "." + result.getReviewerId() + " ");
                    writer.write(rank + " ");
                    writer.write(result.getScore() + " ");
                    writer.write(query.replace(' ', '_') + "\n");
                    ++rank;

                    if (filtered) {
                        if (qRunner.checkSubject(result.getAsin(), query)) {
                            relevant.add(result.getAsin() + "." + result.getReviewerId());
                        } else {
                            notRelevant.add(result.getAsin() + "." + result.getReviewerId());
                        }
                    }
                }

                writer.close();
            } catch (IOException ex) {
                System.err.println("Error writing results file: " + ex.getMessage());
            }
        }

        /**
         * Using relevant and not relevant sets generated in toFile() write qrels file.
         */
        public void writeQrels() {
            File file = new File(filename + "qrels");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                for (String asin : relevant) {
                    writer.write(queryNo + " 0 ");
                    writer.write(asin + " ");
                    writer.write("1\n");
                }

                for (String asin : notRelevant) {
                    writer.write(queryNo + " 0 ");
                    writer.write(asin + " ");
                    writer.write("0\n");
                }

                writer.close();
            } catch (IOException ex) {
                System.err.println("Error writing results file: " + ex.getMessage());
            }
        }
    }
}
