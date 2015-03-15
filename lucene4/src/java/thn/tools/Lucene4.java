package thn.tools;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Lucene4 class
 *
 */
public class Lucene4
{
    private static Logger log = Logger.getLogger(Lucene4.class);

    public static final String CMD_INDEX = "index";
    public static final String CMD_STATUS = "status";
    public static final String CMD_CHECK = "check";
    public static final String CMD_FIELDS = "fields";
    public static final String CMD_SEARCH = "search";
    public static final String CMD_FIND_BYID = "findByID";
    public static final String CMD_FIND_BYCOUNT = "findByCount";
    public static final String CMD_FIND_BYRANGE = "findByRange";

    public static final String FIELD_FILE_PATH = "file-path";
    public static final String FIELD_FILE_SIZE = "file-size";
    public static final String FIELD_FILE_DATE = "file-date";
    public static final String FIELD_FILE_CONTENT = "content";

    private static final Version LUCENE4_VERSION = Version.LUCENE_4_10_4;

    private String indexLocation;

    private IndexWriterConfig indexConfig;
    private Analyzer indexAnalyzer;
    private Directory indexDirectory;
    private final Version indexVersion;

    public Lucene4()
    {
        this.indexVersion = LUCENE4_VERSION;
    }

    public boolean openIndex(final String indexLocation)
    {
        boolean openIndex = false;

        this.indexLocation = indexLocation;
        try {
            this.indexDirectory = FSDirectory.open(new File(indexLocation));
            this.indexAnalyzer = new StandardAnalyzer();
            openIndex = true;
        } catch (final IOException ioe) {
            log.error("Unable to open index at " + indexLocation, ioe);
        }
        return openIndex;
    }

    public void closeIndex() {
        try {
            if (indexDirectory != null) {
                indexDirectory.close();
            }
        } catch (final IOException ioe) {
            log.error("Unable to close index at " + indexLocation, ioe);
        }
    }

    public void index(final String inputLocation, final boolean newIndex) {
        this.indexConfig = new IndexWriterConfig(indexVersion, indexAnalyzer);

        if (newIndex) {
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        try (final IndexWriter indexWriter = new IndexWriter(indexDirectory, indexConfig)) {
            final File dir = new File(inputLocation);
            final File[] files = dir.listFiles();
            for (final File file : files) {
                indexFile(file, indexWriter);
            }
        } catch (final CorruptIndexException cie) {
            log.error("Index is corrupted", cie);
        } catch (final IOException ioe) {
            log.error("Unable to access the index", ioe);
        }
    }

    public void indexFile(final File inputFile, final IndexWriter indexWriter) {
        try {
            log.info("f: " + inputFile.getCanonicalPath());
            final Document document = create(inputFile);

            if (document != null) {
                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    indexWriter.addDocument(create(inputFile));
                } else {
                    indexWriter.updateDocument(new Term(FIELD_FILE_PATH, inputFile.getCanonicalPath()), document);
                }
            }

        } catch (final FileNotFoundException fnfe) {
            log.info("Input file does not exist", fnfe);
        } catch (final IOException ioe) {
            log.info("Invalid input file", ioe);
        }
    }

    public Document create(final File inputFile) throws FileNotFoundException, IOException {
        final Document document = new Document();

        final String path = inputFile.getCanonicalPath();
        document.add(new StringField(FIELD_FILE_PATH, path, Field.Store.YES));

        final Reader reader = new FileReader(inputFile);
        document.add(new TextField(FIELD_FILE_CONTENT, reader));

        final LongField modifiedDate = new LongField(FIELD_FILE_DATE, 0L, Field.Store.YES);
        modifiedDate.setLongValue(inputFile.lastModified());
        document.add(modifiedDate);

        document.add(new LongField(FIELD_FILE_SIZE, inputFile.length(), Field.Store.YES));

        return document;
    }

    public void search(final String queryString, final int numberOfPages, final int resultsPerPage, final boolean displayResults) {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            final QueryParser parser = new QueryParser(FIELD_FILE_CONTENT, indexAnalyzer);
            final Query query = parser.parse(queryString);

            final IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            final TopDocs searchResults = indexSearcher.search(query, (numberOfPages * resultsPerPage));

            log.info("\nSearch Results");
            log.info("- results: " + searchResults.scoreDocs.length);
            if (searchResults.totalHits >= 0) {
                log.info("- total.hits: " + searchResults.totalHits);
            } else {
                log.info("- total.hits: (invalid results)");
            }
            log.info("");

            if (displayResults) {
                final ScoreDoc[] results = searchResults.scoreDocs;
                for (int index = 0; index < results.length; index++) {
                    display(indexSearcher.doc(results[index].doc), results.clone()[index].doc, index);
                }
            }
        } catch (final IOException ioe) {
            log.error("Unable to open the index", ioe);
        } catch (final ParseException pe) {
            log.error("Unable to parse the query", pe);
        }
    }

    private void display(final Document document, final long documentID, final long resultOrder) {
        final List<IndexableField> fieldNames = document.getFields();

        final StringBuilder data = new StringBuilder();
        if (resultOrder >= 0) {
            data.append(String.format("\n[%s] id: %s", resultOrder, documentID));
        } else {
            data.append(String.format("\nid: %s", documentID));
        }

        for (final IndexableField fieldName : fieldNames) {
            data.append(String.format("\n- %s: %s", fieldName.name(), fieldName.stringValue()));
        }
        log.info(data.toString());
        log.info("");
    }

    public void fields() {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            final List<IndexableField> fieldNames = indexReader.document(0).getFields();

            log.info("\nField Names");
            for (final IndexableField fieldName : fieldNames) {
                log.info("- " + fieldName.name());
            }
            log.info("");
        } catch (final IOException ioe) {
            log.error("Unable to open the index", ioe);
        }
    }

    public void status() {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            log.info("\nIndex Status");
            log.info("- numDocs: " + indexReader.numDocs());
            log.info("- numDeletedDocs: " + indexReader.numDeletedDocs());
            log.info("- hasDeletions: " + indexReader.hasDeletions());
            log.info("");
        } catch (final IOException ioe) {
            log.error("Unable to retrieve the status of the index", ioe);
        }
    }

    public void check(final boolean includeSegmentInfo) {
        final CheckIndex checkIndex = new CheckIndex(indexDirectory);
        try {
            final CheckIndex.Status indexStatus = checkIndex.checkIndex();
            log.info("\nIndex Status");
            log.info("- clean: " + indexStatus.clean);
            log.info("- totLoseDocCount: " + indexStatus.totLoseDocCount);
            log.info("- numSegments: " + indexStatus.numSegments);
            log.info("- numBadSegments: " + indexStatus.numBadSegments);
            log.info("- missingSegments: " + indexStatus.missingSegments);

            if (includeSegmentInfo) {
                final List<CheckIndex.Status.SegmentInfoStatus> segmentInfos = indexStatus.segmentInfos;

                log.info("\nSegment Info");
                for (final CheckIndex.Status.SegmentInfoStatus segmentInfo : segmentInfos) {
                    log.info("- segment.name: " + segmentInfo.name);
                    log.info("- openReaderPassed: " + segmentInfo.openReaderPassed);
                    log.info("- sizeMB: " + segmentInfo.sizeMB);

                    if (segmentInfo.openReaderPassed) {
                        log.info("- hasDeletions: " + segmentInfo.hasDeletions);
                        log.info("- docCount: " + segmentInfo.docCount);
                        log.info("- numFiles: " + segmentInfo.numFiles);
                        log.info("- numDeleted: " + segmentInfo.numDeleted);
                    }
                }
            }
            log.info("");
        } catch (final IOException ioe) {
            log.error("Unable to access the index", ioe);
        }
    }

    public void findByID(final int documentID) {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            find(indexReader, documentID, -1);
        } catch (final IOException ioe) {
            log.info("Unable to access the index", ioe);
        }
    }

    public void findByCount(final int startID, final int count) {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            for (int index = 0; index <= count; index++) {
                find(indexReader, (index + startID), index);
            }
        } catch (final IOException ioe) {
            log.info("Unable to access the index", ioe);
        }
    }

    public void findByRange(final int startID, final int endID) {
        try (final IndexReader indexReader = DirectoryReader.open(indexDirectory)) {
            if (endID > startID) {
                for (int index = startID; index <= endID; index++) {
                    find(indexReader, index, index);
                }
            } else {
                log.info(String.format("Invalid range: [%s - %s]", startID, endID));
            }
        } catch (final IOException ioe) {
            log.info("Unable to access the index", ioe);
        }
    }

    public void find(final IndexReader reader, final int documentID, final int documentOrder) {
        try {
            final Document document = reader.document(documentID);
            if (document != null) {
                display(document, documentID, documentOrder);
            }
        } catch (final CorruptIndexException cie) {
            log.error("Index is corrupted", cie);
        } catch (final IOException ioe) {
            log.error(String.format("\nid: %s (invalid)", documentID));
        }
    }

    public void run(final String[] args)
    {
        boolean help = true;

        if (args.length >= 2)
        {
            final String indexLocation = args[0];
            final String command = args[1];

            if (openIndex(indexLocation))
            {

                switch (command)
                {
                case CMD_STATUS:
                    status();
                    help = false;
                    break;

                case CMD_CHECK:
                    boolean includeSegmentInfo = false;
                    if (args.length == 3)
                    {
                        includeSegmentInfo = Boolean.parseBoolean(args[2]);
                    }
                    check(includeSegmentInfo);
                    help = false;
                    break;

                case CMD_FIELDS:
                    fields();
                    help = false;
                    break;

                case CMD_INDEX:
                    boolean createNewIndex = true;
                    if (args.length >= 3)
                    {
                        final String inputLocation = args[2];
                        if (args.length == 4)
                        {
                            createNewIndex = Boolean.parseBoolean(args[3]);
                        }
                        index(inputLocation, createNewIndex);
                        help = false;
                    }
                    break;

                    case CMD_SEARCH:
                        if (args.length >= 3)
                        {
                            boolean displayResults = false;
                            int numberOfPages = 1;
                            int resultsPerPage = 1;

                            final String query = args[2];

                            if (args.length >= 5)
                            {
                                resultsPerPage = Integer.parseInt(args[3]);
                                numberOfPages = Integer.parseInt(args[4]);
                            }

                            if (args.length == 6)
                            {
                                displayResults = Boolean.parseBoolean(args[5]);
                            }
                            search(query, resultsPerPage, numberOfPages, displayResults);
                            help = false;
                        }
                        break;

                case CMD_FIND_BYID:
                    if (args.length == 3)
                    {
                        final int id = Integer.parseInt(args[2]);
                        findByID(id);
                        help = false;
                    }
                    break;

                case CMD_FIND_BYCOUNT:
                    if (args.length == 4)
                    {
                        final int id = Integer.parseInt(args[2]);
                        final int count = Integer.parseInt(args[3]);
                        findByCount(id, count);
                        help = false;
                    }
                    break;

                case CMD_FIND_BYRANGE:
                    if (args.length == 4)
                    {
                        final int startID = Integer.parseInt(args[2]);
                        final int endID = Integer.parseInt(args[3]);
                        findByRange(startID, endID);
                        help = false;
                    }
                    break;

                default:
                    break;
                }
                closeIndex();
            }
            else
            {
                help = false;
            }
        }

        if (help)
        {
            displayHelp();
        }
    }

    public static void main(final String[] args)
    {
        final Lucene4 lucene = new Lucene4();
        lucene.run(args);
    }

    public static void displayHelp()
    {
        final StringBuilder help = new StringBuilder();
        help.append("\nOptions: <index location> <command> <command options>");
        help.append("\n- <index location> findByID <document id>");
        help.append("\n- <index location> findByCount <start id> <count>");
        help.append("\n- <index location> findByRange <start id> <end id>");
        help.append("\n---");
        help.append("\n- <index location> status");
        help.append("\n- <index location> check [true (display segment info)]");
        help.append("\n---");
        help.append("\n- <index location> index <input location> [true (create new index)]");

        System.out.println(help.toString());
    }
}