package co.uk.andrespedraza.java.awsresourcesupload;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Paths;

public class UploadForm {

    private JTextField textFieldSecret;
    private JTextField textFieldClientId;
    private JTextField textFieldFolder;
    private JTextField textFieldBucket;
    private JTextField textFieldResources;
    private JTextField textFieldOutput;
    private JButton browseButtonResources;
    private JButton browseButtonOutput;
    private JButton startUploadButton;
    private JPanel contentPane;
    private JLabel labelLogo;
    public UploadForm() {

        String defaultOutputFolder = Paths.get(".").toAbsolutePath().normalize().toString();
        this.textFieldOutput.setText(defaultOutputFolder);

        String defaultUserFolder = System.getProperty("user.home");
        this.browseButtonResources.addActionListener(
                (ActionEvent e) -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File(defaultUserFolder));
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int returnValue = fileChooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        String selectedResourcesFolder = fileChooser.getSelectedFile().getAbsolutePath();
                        this.textFieldResources.setText(selectedResourcesFolder);
                    }
                }
        );

        this.browseButtonOutput.addActionListener(
                (ActionEvent e) -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File(defaultOutputFolder));
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int returnValue = fileChooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        String selectedOutputFolder = fileChooser.getSelectedFile().getAbsolutePath();
                        this.textFieldOutput.setText(selectedOutputFolder);
                    }
                }
        );

        this.startUploadButton.addActionListener(
                (ActionEvent e) -> new UploadWorker(UploadForm.this).execute()
        );


    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("AWS Resources upload");
        frame.setContentPane(new UploadForm().contentPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    protected void changeButtonStatus(boolean active) {
        new Thread(() -> {
            String text = active ? "Start upload" : "Uploading...";
            startUploadButton.setText(text);
            startUploadButton.setEnabled(active);
        }).start();
    }

    public JTextField getTextFieldSecret() {
        return textFieldSecret;
    }

    public JTextField getTextFieldClientId() {
        return textFieldClientId;
    }

    public JTextField getTextFieldFolder() {
        return textFieldFolder;
    }

    public JTextField getTextFieldBucket() {
        return textFieldBucket;
    }

    public JTextField getTextFieldResources() {
        return textFieldResources;
    }

    public JTextField getTextFieldOutput() {
        return textFieldOutput;
    }
}
