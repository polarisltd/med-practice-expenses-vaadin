package lv.polarisit;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        // Create the drawer toggle
        DrawerToggle toggle = new DrawerToggle();

        // Create the navigation links
        RouterLink mainViewLink = new RouterLink("Main View", MainView.class);
        RouterLink uploadViewLink = new RouterLink("Upload View", UploadView.class);

        // Add the toggle and links to the drawer
        addToDrawer(new Span(mainViewLink));
        addToDrawer(new Span(uploadViewLink));

        // Add the toggle to the header
        addToNavbar(toggle);
    }
}
