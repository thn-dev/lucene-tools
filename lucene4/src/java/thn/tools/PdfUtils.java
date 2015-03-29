package thn.tools;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class PdfUtils
{
    private static final Logger log = Logger.getLogger(PdfUtils.class);

    public static String pdfToText(final File pdfFile)
    {
        String textContent = null;
        try
        {
            final PDDocument pdDocument = PDDocument.load(pdfFile);
            final PDFTextStripper textStripper = new PDFTextStripper();
            textContent = textStripper.getText(pdDocument);
            pdDocument.close();
        }
        catch (final IOException e)
        {
            log.error("Invalid PDf file", e);
        }
        return textContent;
    }
}
