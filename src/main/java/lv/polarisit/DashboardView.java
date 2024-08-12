package lv.polarisit;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lv.polarisit.data.Akts;
import lv.polarisit.data.AktsRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    public DashboardView(AktsRepository aktsRepository) {
        setSpacing(true);
        setPadding(true);

        List<Akts> aktsList = aktsRepository.findAll();
        Map<String, List<Akts>> groupedByMonth = aktsList.stream()
                .collect(Collectors.groupingBy(akts -> akts.getDocDate().format(DateTimeFormatter.ofPattern("yyyy/MM"))));

        groupedByMonth.forEach((month, akts) -> {
            int numberOfDocuments = akts.size();
            double totalAmount = akts.stream().mapToDouble(a -> a.getDocAmount().doubleValue()).sum();

            Div card = createCard(month, numberOfDocuments, totalAmount);
            add(card);
        });

        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private Div createCard(String month, int numberOfDocuments, double totalAmount) {
        Div card = new Div();
        card.addClassName("card");

        Div monthDiv = new Div();
        monthDiv.setText("Month: " + month);

        Div numberOfDocumentsDiv = new Div();
        numberOfDocumentsDiv.setText("Number of Documents: " + numberOfDocuments);

        Div totalAmountDiv = new Div();
        totalAmountDiv.setText("Total Amount: " + totalAmount);

        card.add(monthDiv, numberOfDocumentsDiv, totalAmountDiv);
        card.getStyle().set("border", "1px solid #ccc");
        card.getStyle().set("padding", "10px");
        card.getStyle().set("margin", "10px");
        card.getStyle().set("width", "200px");

        return card;
    }
}