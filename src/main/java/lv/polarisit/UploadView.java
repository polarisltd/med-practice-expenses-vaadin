/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lv.polarisit;


import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import lv.polarisit.data.Akts;
import lv.polarisit.data.AktsRepository;
import lv.polarisit.word.WordTemplateProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pdfconverter.PdfToImageConverter;
import pl.allegro.finance.tradukisto.MoneyConverters;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * View for {@link Upload} demo.
 *
 * @author Vaadin Ltd
 */


@Route(value = "upload", layout = MainLayout.class)
public class UploadView extends HorizontalLayout {
    PropertyHolder propertyHolder;
    private final AktsRepository aktsRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadView.class);
    private final ListBox<String> personName = new ListBox<>();
    private final Grid<Akts> gridAkts = new Grid<>(Akts.class);
    private final DatePicker docDate = new DatePicker("Document date");
    private final TextField docAmount = new TextField("Amount");
    private final Button saveButton = new Button("Save");
    private final Button convertButton = new Button("Convert to word");
    private final BeanValidationBinder<Akts> binder = new BeanValidationBinder<>(Akts.class);
    private final Button clearButton = new Button("Clear");
    private Div imageContent;
    private String imageFilename;
    private List<Path> imagePaths;


    public UploadView(PropertyHolder propertyHolder, AktsRepository aktsRepository) {
        this.propertyHolder = propertyHolder;
        this.aktsRepository = aktsRepository;
        personName.setItems("Laimdota Berģīte", "Dora Berģīte");
        personName.setValue(getFirstPersonItem());
        var form = new VerticalLayout();
        // Bind fields
        binder.bind(personName, Akts::getPersonName, Akts::setPersonName);
        binder.bind(docDate, Akts::getDocDate, Akts::setDocDate);
        binder.bind(docAmount, akts -> akts.getDocAmount() != null ? akts.getDocAmount().toString() : "", (akts, amountStr) -> akts.setDocAmount(amountStr != null && !amountStr.isEmpty() ? new BigDecimal(amountStr) : null));

        saveButton.addClickListener(event -> {
            Akts akts = new Akts();
            try {
                BinderValidationStatus<Akts> validationStatus = binder.validate();
                if (validationStatus.isOk()) {
                    binder.writeBean(akts);
                    akts.setReceiptImagePath(this.imageFilename);
                    aktsRepository.save(akts);
                    Notification.show("Saved", 3000, Notification.Position.MIDDLE);
                    populateGrid();
                } else {
                    String errorMessages = validationStatus.getFieldValidationErrors().stream().map(BindingValidationStatus::getMessage).filter(Optional::isPresent).map(Optional::get).collect(Collectors.joining(", "));
                    Notification.show("Validation failed: " + errorMessages, 3000, Notification.Position.MIDDLE);
                }

            } catch (Exception e) {
                Notification.show("error: %s".formatted(e.getMessage()), 3000, Notification.Position.MIDDLE);
            }
        });
        gridAkts.addColumn(new ComponentRenderer<>(akts -> {
            Button deleteButton = new Button("Delete");
            deleteButton.addClickListener(event -> {
                aktsRepository.delete(akts);
                populateGrid();
                Notification.show("Deleted", 3000, Notification.Position.MIDDLE);
            });
            return deleteButton;
        })).setHeader("Actions");
        gridAkts.getColumnByKey("receiptImagePath").setRenderer(new ComponentRenderer<>(akts -> {
            Div cell = new Div();
            cell.setText(akts.getReceiptImagePath());
            cell.getElement().setProperty("title", akts.getReceiptImagePath()); // Set tooltip
            return cell;
        }));
        gridAkts.getColumnByKey("docDate").setRenderer(new ComponentRenderer<>(akts -> {
            Div cell = new Div();
            cell.setText(akts.getDocDate().toString());
            cell.getElement().setProperty("title", akts.getDocDate().toString()); // Set tooltip
            return cell;
        }));
        convertButton.addClickListener(event -> {

            String templatePath = propertyHolder.getWordTemplatePath();
            Akts akts = new Akts();
            try {
                binder.writeBean(akts);
            } catch (Exception e) {
                LOGGER.debug("Exception occurred while writing Entity bean", e);
            }
            String docFilename = getDocFilename(akts) + ".docx";
            String outputPath = Paths.get(propertyHolder.getWordOutputPath(), docFilename).toString();
            Map<String, String> replacements = new HashMap<>();

            var bdValue = new BigDecimal(docAmount.getValue());
            MoneyConverters moneyConverter = MoneyConverters.LATVIAN_BANKING_MONEY_VALUE;
            String valueAsWords = moneyConverter.asWords(bdValue);
            replacements.put("${name}", personName.getValue());
            replacements.put("${date}", docDate.getValue().toString());
            replacements.put("${date-wording}", dateWording(docDate.getValue()));
            replacements.put("${amount}", docAmount.getValue());
            replacements.put("${wording}", valueAsWords);
            XWPFPictureData.setMaxImageSize(100_000_000); // check is working
            LOGGER.info("maxImageSize%s".formatted(XWPFPictureData.getMaxImageSize()));
            WordTemplateProcessor.process(templatePath, outputPath, replacements, imagePaths);

            StreamResource resource = new StreamResource(docFilename, () -> {
                try {
                    return Files.newInputStream(Paths.get(outputPath));
                } catch (IOException e) {
                    LOGGER.error("Error reading file", e);
                    return null;
                }
            });

            Anchor downloadLink = new Anchor(resource, "Download Converted Document");
            downloadLink.getElement().setAttribute("download", true);

            form.add(downloadLink);
            Notification.show("Document converted", 3000, Notification.Position.MIDDLE);
        });

        clearButton.addClickListener(event -> {
            docDate.clear();
            personName.clear();
            docAmount.clear();
            imageContent.removeAll();
            imageFilename = null;
            imagePaths = List.of();
            Notification.show("Fields cleared", 3000, Notification.Position.MIDDLE);
        });

        form.add(new H1("Upload"), clearButton, docDate, personName, docAmount, createSimpleUpload(), saveButton, convertButton);
        this.add(form);
        var grid = new VerticalLayout();
        grid.add(new H1("Browser"), gridAkts);
        this.add(form, grid);

        populateGrid();
    }

    private void populateGrid() {
        List<Akts> aktsList = aktsRepository.findAll();
        gridAkts.setItems(aktsList);
    }

    private String dateWording(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. 'gada' dd. MMMM.", new Locale("lv", "LV"));
        String formattedDate = date.format(formatter);
        LOGGER.info(formattedDate); // Output: 1 augusts, 2024
        return formattedDate;
    }

    private Div createSimpleUpload() {
        Div output = new Div();

        //@formatter:off
  // begin-source-example
  // source-example-heading: Simple in memory receiver for single file upload
  MemoryBuffer buffer = new MemoryBuffer();
  Upload upload = new Upload(buffer);
  upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "application/pdf");
  upload.setAutoUpload(true);

  upload.addSucceededListener(event -> {
  this.imageFilename = event.getFileName();
  Component imageComponent = createComponent(
        event.getMIMEType(),
        event.getFileName(), buffer.getInputStream());

  showOutput(imageFilename, imageComponent, output);
  });
  // end-source-example
  //@formatter:on
        upload.setMaxFileSize(1024 * 1024 * 1024);
        upload.setId("test-upload");

        output.setId("test-output");

        output.add(new Div("Simple in memory receiver for single file upload"), upload);

        //addCard("Simple in memory receiver for single file upload", upload,
        //  output);
        return output;
    }

    public String getDocFilename(Akts akts) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        String formattedDate = akts.getDocDate().format(dateFormatter);

        return "%s_%s_%s_%s".formatted(formattedDate, "LBAP_akts", akts.getPersonName().replace(" ", "-"), akts.getDocAmount().toPlainString().replace(".", "-"));
    }


    public String getFirstPersonItem() {
        return personName.getListDataView().getItems().findFirst().orElse(null);
    }

    private Component createComponent(String mimeType, String fileName, InputStream stream) {
        if (mimeType.startsWith("text")) {
            return createTextComponent(stream);
        } else if (mimeType.startsWith("image")) {
            var documentDate = docDate.getValue();
            var person = personName.getValue();

            try {


                byte[] bytes = IOUtils.toByteArray(stream);

                Path imagePath = Paths.get(this.propertyHolder.getImagePath(), imageFilename);
                this.imagePaths = List.of(imagePath);
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath, bytes);
                LOGGER.info("Image uploaded to " + imagePath.toAbsolutePath());

                return createImage(new ByteArrayInputStream(bytes));


            } catch (IOException e) {
                LOGGER.error("Error reading image", e);
            }


        } else if (mimeType.equals("application/pdf")) {
            try {
                byte[] bytes = IOUtils.toByteArray(stream);
                Path pdfPath = Paths.get(this.propertyHolder.getImagePath(), imageFilename);
                Files.createDirectories(pdfPath.getParent());
                Files.write(pdfPath, bytes); // write pdf to file
                LOGGER.info("Pdf uploaded to " + pdfPath.toAbsolutePath());
                List<String> images = PdfToImageConverter.convertPdfToImages(pdfPath.toAbsolutePath().toString(), this.propertyHolder.getImagePath());
                Div outputContainer = new Div();
                imagePaths = new ArrayList<>();
                images.forEach(thisImagePath -> {
                    LOGGER.info("image uploaded to: %s".formatted(thisImagePath));
                    imagePaths.add(Paths.get(thisImagePath));
                    InputStream is = createInputStreamFromFile(thisImagePath);
                    Image image = createImage(is);
                    image.setWidth("600px");
                    Div imageContainer = new Div(image);
                    outputContainer.add(imageContainer);
                });
                return outputContainer;
            } catch (IOException e) {
                LOGGER.error("Error reading image", e);
            }

        }
        Div content = new Div();
        String text = String.format("Mime type: '%s'\nSHA-256 hash: '%s'", mimeType, MessageDigestUtil.sha256(stream.toString()));
        content.setText(text);
        return content;

    }

        public static InputStream createInputStreamFromFile(String absoluteFilePath) {
            try {
                Path path = Paths.get(absoluteFilePath);
                return Files.newInputStream(path);
            } catch (IOException e) {
                LOGGER.error("Error reading file", e);
                return new ByteArrayInputStream(new byte[0]);
            }
        }

    Image createImage(InputStream stream) {
        Image image = new Image();
        try {

            byte[] bytes = IOUtils.toByteArray(stream);

            Path imagePath = Paths.get(this.propertyHolder.getImagePath(), imageFilename);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, bytes);
            LOGGER.info("Image uploaded to " + imagePath.toAbsolutePath());

            image.getElement().setAttribute("src", new StreamResource(imageFilename, () -> new ByteArrayInputStream(bytes)));
            try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        image.setWidth("300px");
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error reading image", e);
        }
        return image;
    }


    private Component createTextComponent(InputStream stream) {
        String text;
        try {
            text = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            text = "exception reading stream";
        }
        return new Text(text);
    }

    private void showOutput(String text, Component content, HasComponents outputContainer) {
        HtmlComponent p = new HtmlComponent(Tag.P);
        outputContainer.add(p);
        imageContent = new Div(new Div(text), content);
        outputContainer.add(imageContent);
    }
}
