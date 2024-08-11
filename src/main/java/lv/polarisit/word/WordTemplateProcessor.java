package lv.polarisit.word;


import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
/*

Identify the file format:

OLE2 Format:  such as .doc for Word documents.
OOXML Format:  such as .docx for Word documents.

Use the appropriate POI class:

OLE2 Format: Use HWPFDocument for .doc files.
OOXML Format: Use XWPFDocument for .docx files.

Solution to get OOXML document working with code.
OOXML: LibreOffice export format 2010/office-365 format

*/



public class WordTemplateProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordTemplateProcessor.class);
    public static void process(String templatePath, String outputPath, Map<String, String> replacements, Path imagePath) {
        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Replace text placeholders
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text != null) {
                        for (Map.Entry<String, String> entry : replacements.entrySet()) {
                            text = text.replace(entry.getKey(), entry.getValue());
                        }
                        run.setText(text, 0);
                    }
                }
            }

            // Replace image placeholder
            insertImage(document, imagePath);

            // Save the modified document
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                document.write(fos);
            }

        } catch (Exception e) {
            LOGGER.error("Error processing Word template", e);
        }
    }

    private static void insertImage(XWPFDocument document, Path imagePath) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null && text.contains("${image}")) {
                    run.setText("", 0); // Clear the placeholder text
                    int width=0,height=0;
                    try (FileInputStream imageStream = new FileInputStream(imagePath.toString())) {
                        BufferedImage bimg = ImageIO.read(imageStream);
                        width = bimg.getWidth();
                        height = bimg.getHeight();

                    }catch (Exception e) {
                        LOGGER.error("error determining image file extension",e);
                    }
                    try (FileInputStream imageStream = new FileInputStream(imagePath.toString())) {
                        LOGGER.info("Inserting image: {}", imagePath);
                        run.addPicture(imageStream,
                                getPictureType(getFileExtension(imagePath)),
                                imagePath.getFileName().toString(),
                                Units.toEMU(Math.min(width, 600)),
                                Units.toEMU(Math.min(height, 600)));
                    } catch (Exception e) {
                        LOGGER.error("error determining image file extension",e);
                    }
                }
            }
        }
    }




        public static int getPictureType(String fileExtension) {
            switch (fileExtension.toLowerCase()) {
                case "emf":
                    return XWPFDocument.PICTURE_TYPE_EMF;
                case "wmf":
                    return XWPFDocument.PICTURE_TYPE_WMF;
                case "pict":
                    return XWPFDocument.PICTURE_TYPE_PICT;
                case "jpeg":
                case "jpg":
                    return XWPFDocument.PICTURE_TYPE_JPEG;
                case "png":
                    return XWPFDocument.PICTURE_TYPE_PNG;
                case "dib":
                    return XWPFDocument.PICTURE_TYPE_DIB;
                case "gif":
                    return XWPFDocument.PICTURE_TYPE_GIF;
                case "tiff":
                case "tif":
                    return XWPFDocument.PICTURE_TYPE_TIFF;
                case "eps":
                    return XWPFDocument.PICTURE_TYPE_EPS;
                case "bmp":
                    return XWPFDocument.PICTURE_TYPE_BMP;
                case "wpg":
                    return XWPFDocument.PICTURE_TYPE_WPG;
                case "svg":
                    return XWPFDocument.PICTURE_TYPE_SVG;
                default:
                    throw new IllegalArgumentException("Unsupported picture type: " + fileExtension);
            }
        }

    public static String getFileExtension(Path path) {
        String fileName = path.toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return ""; // No extension found
        }
        return fileName.substring(dotIndex + 1);
    }


}