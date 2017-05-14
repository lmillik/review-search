/**
 * Author: Lowell Milliken
 * Date: 5/13/2017
 * For: CSC 849 Term Project
 * Description: This program will index review data using Apache Lucene.
 */
package indexing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * This class can create an inverted index given a documents file where each
 * where each document is separated by {@code <DOC #> </DOC>} tags. The
 * resulting index is stored in a file.
 *
 * @author Lowell Milliken
 */
public class IndexGenerator {

    private final File documentsFile;       // The documents source file

    /**
     * Nothing special here. Just setting the documents file.
     *
     * @param docsFile The documents source file
     */
    public IndexGenerator(File docsFile) {
        this.documentsFile = docsFile;
    }

    /**
     * Creates an index using Lucene. If filter = true, create a filtered index using
     * the books index. The books index must already exist for this to work.
     *
     * @param indexFile reference to the directory
     * @param filter create a filtered index or not
     */
    public void createIndex(File indexFile, boolean filter) {

        try {
            // searcher for the book index
            IndexSearcher searcher = null;
            
            // open a searcher for the book index if filtering
            if(filter) {
                File bindexFile = new File(IndexConstants.BOOK_INDEX_LOCATION);
                Directory bdir = FSDirectory.open(bindexFile.toPath());
                IndexReader breader = DirectoryReader.open(bdir);
                searcher = new IndexSearcher(breader);
                searcher.setSimilarity(new BM25Similarity());
            }
            
            // open documents file
            BufferedReader reader = new BufferedReader(new FileReader(documentsFile));
            JsonFactory jsonFactory = new JsonFactory();

            // creating index writer
            Directory dir = FSDirectory.open(indexFile.toPath());
            IndexWriterConfig config = new IndexWriterConfig(new EnglishAnalyzer()); // use english analyzer for stemming and stopwords
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); // append if the index exists
            config.setSimilarity(new BM25Similarity()); // use BM25 similarity
            IndexWriter writer = new IndexWriter(dir, config);

            String line;
            
            // create new field type for review text
            // need the term vectors for Rocchio and PRF
            FieldType reviewFieldType = new FieldType();
            reviewFieldType.setStoreTermVectors(true);
            reviewFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            reviewFieldType.setTokenized(true);
            reviewFieldType.setStored(true);
            reviewFieldType.freeze();
            
            // each line contains a JSON string
            // parse each json and create a document object to be added to the index
            while ((line = reader.readLine()) != null) {
                JsonParser jsonParser = jsonFactory.createParser(line);
                jsonParser.setCodec(new ObjectMapper());
                Document doc = new Document();
                while (!jsonParser.isClosed()) {
                    jsonParser.nextToken();
                    String fieldname = jsonParser.getCurrentName();
                    if (fieldname != null) {
                        switch (fieldname) {
                            case IndexConstants.TEXT:
                                jsonParser.nextToken();
                                doc.add(new Field(IndexConstants.TEXT, jsonParser.getText(), reviewFieldType));
                                break;
                            case IndexConstants.ASIN:
                                jsonParser.nextToken();
                                doc.add(new StringField(IndexConstants.ASIN, jsonParser.getText(), Field.Store.YES));
                                break;
                            case IndexConstants.REVIEWER_ID:
                                jsonParser.nextToken();
                                doc.add(new StringField(IndexConstants.REVIEWER_ID, jsonParser.getText(), Field.Store.YES));
                                break;
                            case IndexConstants.SUMMARY:
                                jsonParser.nextToken();
                                doc.add(new TextField(IndexConstants.SUMMARY, jsonParser.getText(), Field.Store.YES));
                                break;
                            case IndexConstants.REVIEWER_NAME:
                                jsonParser.nextToken();
                                doc.add(new StringField(IndexConstants.REVIEWER_NAME, jsonParser.getText(), Field.Store.YES));
                                break;
                            default:
                                break;
                        }
                    }
                }
                
                // if the filter is on
                // attempt to find the ASIN in the books index
                // if there are no results, do not add to index
                if(filter) {
                    String asin = doc.getField(IndexConstants.ASIN).stringValue();
                    TopDocs topDocs = searcher.search(new TermQuery(new Term(IndexConstants.ASIN, asin)), 1);
                    if(topDocs.totalHits > 0) {
                        writer.addDocument(doc);
                    }
                } else {
                    writer.addDocument(doc);
                }
            }

            writer.commit();
            writer.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Could not find documents file: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IO error: " + ex.getMessage());
        }
    }

    /**
     * Creates a book index given a book metadata file.
     * 
     * @param books book metadata file
     * @param indexFile book index directory
     */
    public void createBookIndex(File books, File indexFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(books));
            Directory dir = FSDirectory.open(indexFile.toPath());
            IndexWriterConfig config = new IndexWriterConfig(new EnglishAnalyzer());
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            config.setSimilarity(new BM25Similarity());
            IndexWriter writer = new IndexWriter(dir, config);
            
            ObjectMapper mapper = new ObjectMapper();
            String line;
            while ((line = reader.readLine()) != null) {
                Document doc = new Document();
                JsonNode node = mapper.readTree(line);
                node = node.path("data");
                node = node.path(0);

                // concatenate authors into a single string
                JsonNode authorNode = node.path("author_data");
                Iterator<JsonNode> it = authorNode.elements();
                String authors = "";
                while(it.hasNext()) {
                    JsonNode author = it.next();
                    authors += author.path("name").asText() + '\n';
                }
                doc.add(new TextField(IndexConstants.AUTHOR, authors, Field.Store.YES));

                doc.add(new TextField(IndexConstants.TITLE, node.path("title_latin").asText(), Field.Store.YES));
                
                // concatenate subjects into a single string
                JsonNode subjectNode = node.path("subject_ids");
                Iterator<JsonNode> its = subjectNode.elements();
                String subjects = "";
                while(its.hasNext()) {
                    JsonNode subject = its.next();
                    subjects += subject.asText().replace('_', ' ') + '\n';
                }
                doc.add(new TextField(IndexConstants.SUBJECT, subjects, Field.Store.YES));
                
                doc.add(new StringField(IndexConstants.ASIN, node.path("isbn10").asText().trim(), Field.Store.YES));
                
                writer.addDocument(doc);
            }
            
            writer.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Could not find books file: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IO error: " + ex.getMessage());
        }
        
    }
}
