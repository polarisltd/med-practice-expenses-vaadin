package pdfconverter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PdfToImageConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfToImageConverter.class);
    public static List<String> convertPdfToImages(String pdfPath, String outputDir)  {
        List<String> convertedImages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300);
                String imageFilename = outputDir + "/" + extractFileNameWithoutExtension(pdfPath)+"_" + (page + 1) + ".png";
                ImageIO.write(bim, "png", new File(imageFilename));
                convertedImages.add(imageFilename);
            }
        }catch(IOException e){
            LOGGER.error("Error converting pdf to images", e);
            return List.of();
        }
        return convertedImages;
    }
    public static String extractFileNameWithoutExtension(String absoluteFilePath) {
        Path path = Paths.get(absoluteFilePath);
        String fileName = path.getFileName().toString();
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex);
        } else {
            return fileName; // No extension found
        }
    }
}
