package thn.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneMultiSearch
{
    private final Logger log = Logger.getLogger(LuceneMultiSearch.class);

    private final String indexLocation;
    private final List<IndexReader> readers;
    private final Version indexVersion;
    private final Analyzer indexAnalyzer;

    public LuceneMultiSearch()
    {
        this.indexLocation = null;
        this.readers = null;
        this.indexVersion = null;
        this.indexAnalyzer = null;
    }

    public LuceneMultiSearch(final Version indexVersion, final String indexLocation)
    {
        this.indexVersion = indexVersion;
        this.indexLocation = indexLocation;
        this.indexAnalyzer = new StandardAnalyzer(indexVersion);
        this.readers = new ArrayList<>();
    }

    private void init()
    {
        final File dir = new File(indexLocation);
        final File[] files = dir.listFiles();
        for (final File file : files)
        {
            if (file.isDirectory())
            {
                try
                {
                    final IndexReader reader = IndexReader.open(FSDirectory.open(file));
                    readers.add(reader);
                }
                catch (final IOException ioe)
                {
                    log.info("Unable to read " + file.getPath(), ioe);
                }
            }
        }
    }

    public List<Document> search(final String queryString, final int resultsPerPage, final int numberOfPages)
    {
        final List<Document> docs = new ArrayList<>();

        try (final MultiReader multiReader = new MultiReader((IndexReader[])readers.toArray());
                final IndexSearcher indexSearcher = new IndexSearcher(multiReader);)
        {
            final QueryParser parser = new QueryParser(indexVersion, Lucene3.FIELD_FILE_CONTENT, indexAnalyzer);
            final Query query = parser.parse(queryString);

            final TopDocs searchResults = indexSearcher.search(query, (numberOfPages * resultsPerPage));

            log.info("\nSearch Results");
            log.info("- results: " + searchResults.scoreDocs.length);
            if (searchResults.totalHits >= 0)
            {
                log.info("- total.hits: " + searchResults.totalHits);
            }
            else
            {
                log.info("- total.hits: (invalid results)");
            }
            log.info("");

            final ScoreDoc[] results = searchResults.scoreDocs;
            for (int index = 0; index < results.length; index++)
            {
                docs.add(indexSearcher.doc(results[index].doc));
            }
        }
        catch (final IOException ioe)
        {
            log.error("Unable to open the index", ioe);
        }
        catch (final ParseException pe)
        {
            log.error("Unable to parse the query", pe);
        }
        return docs;
    }
}
