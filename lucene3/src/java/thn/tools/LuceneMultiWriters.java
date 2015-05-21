package thn.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneMultiWriters
{
    private final Logger log = Logger.getLogger(LuceneMultiSearch.class);

    private final String indexLocation;
    private final List<IndexWriter> writers;
    private final IndexWriterConfig writerConfig;
    private final Version indexVersion;

    public LuceneMultiWriters()
    {
        this.indexLocation = null;
        this.writers = null;
        this.writerConfig = null;
        this.indexVersion = null;
    }

    public LuceneMultiWriters(final Version indexVersion, final String indexLocation)
    {
        this.indexVersion = indexVersion;
        this.indexLocation = indexLocation;
        this.writerConfig = new IndexWriterConfig(indexVersion, new StandardAnalyzer(indexVersion));

        this.writers = new ArrayList<>();
    }

    public void init(final int numberOfIndices)
    {
        final File dir = new File(indexLocation);
        final File[] files = dir.listFiles();
        for (final File file : files)
        {
            if (file.isDirectory())
            {
                try
                {
                    final IndexWriter writer = new IndexWriter(FSDirectory.open(file), writerConfig);
                    writers.add(writer);
                }
                catch (final IOException ioe)
                {
                    log.info("Unable to read " + file.getPath(), ioe);
                }
            }
        }
    }

    public void init()
    {
        final File dir = new File(indexLocation);
        final File[] files = dir.listFiles();
        for (final File file : files)
        {
            if (file.isDirectory())
            {
                try
                {
                    final IndexWriter writer = new IndexWriter(FSDirectory.open(file), writerConfig);
                    writers.add(writer);
                }
                catch (final IOException ioe)
                {
                    log.info("Unable to read " + file.getPath(), ioe);
                }
            }
        }
    }
}
