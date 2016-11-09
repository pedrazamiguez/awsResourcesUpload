package co.uk.andrespedraza.java.awsresourcesupload;

import javax.swing.*;

public class Application {

    public static void main(String... args) {

        // 1. Set OS look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("I couldn't set the OS look and feel!");
        }

        // 2. Launch window.
        UploadForm.main(args);

    }

}
