package lv.polarisit;

import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class MainView extends VerticalLayout {

    public MainView(@Autowired GreetService service) {
        TextField textField = new TextField("Your name");
        textField.addClassName("bordered");

        Button button = new Button("Say hello", e -> {
            add(new Paragraph(service.greet(textField.getValue())));
        });

        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickShortcut(Key.ENTER);

        addClassName("centered-content");

        add(textField, button);
    }
}