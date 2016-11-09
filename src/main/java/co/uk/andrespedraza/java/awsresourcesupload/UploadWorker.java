package co.uk.andrespedraza.java.awsresourcesupload;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadWorker extends SwingWorker<Integer, Integer> {

    private static final String AWS_S3_ENDPOINT = "https://s3.amazonaws.com";
    private static final String DEFAULT_FILE_TYPE = "application/octet-stream";
    private static final Map<String, String> fileExtensionMap;

    static {
        fileExtensionMap = new HashMap<>();

        fileExtensionMap.put("gif", "image/gif");
        fileExtensionMap.put("png", "image/png");
        fileExtensionMap.put("jpg", "image/jpg");
        fileExtensionMap.put("jpeg", "image/jpeg");
    }

    private UploadForm form;

    public UploadWorker(UploadForm form) {
        this.form = form;
    }

    public static byte[] createThumbnail(byte[] originalImage, String fileExtension) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalImage));
            BufferedImage scaledImg = Scalr.resize(img, Scalr.Method.QUALITY,
                    50, 50, Scalr.OP_ANTIALIAS);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaledImg, fileExtension, baos);
            return baos.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Integer doInBackground() throws Exception {
        // 1. Disable button
        form.changeButtonStatus(false);

        // 2. Get parameters
        String awsClientId = form.getTextFieldClientId().getText();
        String awsClientSecret = form.getTextFieldSecret().getText();
        String awsBucket = form.getTextFieldBucket().getText();
        String awsBucketFolder = form.getTextFieldFolder().getText();
        String resourcesFolder = form.getTextFieldResources().getText();
        String outputFolder = form.getTextFieldOutput().getText();

        // 3. Validate
        String errorMessage = "";
        if (awsClientId.isEmpty()) {
            errorMessage += "AWS Client Id cannot be empty.\n";
        }
        if (awsClientSecret.isEmpty()) {
            errorMessage += "AWS Client secret cannot be empty.\n";
        }
        if (awsBucket.isEmpty()) {
            errorMessage += "S3 bucket cannot be empty.\n";
        }
        if (awsBucketFolder.isEmpty()) {
            errorMessage += "S3 folder cannot be empty.\n";
        }
        if (resourcesFolder.isEmpty()) {
            errorMessage += "Resources folder cannot be empty.\n";
        }
        if (outputFolder.isEmpty()) {
            errorMessage += "Output folder cannot be empty.\n";
        }
        File resourcesFolderFile = new File(resourcesFolder);
        if (!resourcesFolderFile.exists() || !resourcesFolderFile.isDirectory()) {
            errorMessage += "Resources folder specified is not valid.\n";
        }
        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists() || !outputFolderFile.isDirectory()) {
            errorMessage += "Output folder specified is not valid.\n";
        }

        if (!errorMessage.isEmpty()) {
            JOptionPane.showMessageDialog(null, errorMessage, "Validation errors", JOptionPane.ERROR_MESSAGE);
        } else {

            // 4. Prepare upload.
            List<String> lstOriginalUrls = new ArrayList<>();
            List<String> lstThumbnailUrls = new ArrayList<>();
            BasicAWSCredentials credentials = new BasicAWSCredentials(awsClientId, awsClientSecret);
            AmazonS3Client s3Client = new AmazonS3Client(credentials);
            s3Client.setEndpoint(AWS_S3_ENDPOINT);

            // 5. Get files from resources folder.
            List<File> filesToUpload = findFilesInFolder(resourcesFolderFile);
            if (!filesToUpload.isEmpty()) {

                for (File image : filesToUpload) {
                    // 6. Create thumbnail
                    String fileExtension = getFileExtension(image);
                    String originalFileName = getFileName(awsBucketFolder, image, false);
                    String thumbnailFileName = getFileName(awsBucketFolder, image, true);
                    byte[] originalImage = Files.readAllBytes(image.toPath());
                    byte[] imageThumbnail = createThumbnail(originalImage, fileExtension);

                    // 7. Upload
                    String originalImageUrl = uploadFileToAWS(s3Client, originalImage, awsBucket, originalFileName, fileExtension);
                    System.out.println(originalImageUrl);
                    String imageThumbnailUrl = uploadFileToAWS(s3Client, imageThumbnail, awsBucket, thumbnailFileName, fileExtension);
                    System.out.println(imageThumbnailUrl);

                    lstOriginalUrls.add(originalImageUrl);
                    lstThumbnailUrls.add(imageThumbnailUrl);
                }



            } else {
                JOptionPane.showMessageDialog(null, "No files found!", "Validation errors", JOptionPane.ERROR_MESSAGE);
            }

        }

        return null;
    }

    @Override
    protected void done() {
        super.done();
        form.changeButtonStatus(true);
    }

    @Override
    protected void process(List<Integer> chunks) {
        super.process(chunks);
    }

    private String uploadFileToAWS(AmazonS3Client s3Client, byte[] imageContent, String bucketName, String fileName, String fileExtension) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageContent.length);
        metadata.setContentType(fromExtensionToContentType(fileExtension));
        PutObjectRequest request = new PutObjectRequest(bucketName, fileName, new ByteArrayInputStream(imageContent), metadata);
        request.setCannedAcl(CannedAccessControlList.PublicRead);
        s3Client.putObject(request);
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
    }

    private String getFileExtension(File file) {
        return FilenameUtils.getExtension(file.getName());
    }

    private String fromExtensionToContentType(String extension) {
        if (null != extension) {
            extension = extension.toLowerCase();
        }
        String contentType = fileExtensionMap.get(extension);
        if (null == contentType) {
            contentType = DEFAULT_FILE_TYPE;
        }
        return contentType;
    }

    private List<File> findFilesInFolder(File folder) {
        List<File> lstFiles = new ArrayList<>();

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                lstFiles.add(fileEntry);
            }
        }

        return lstFiles;
    }

    private String getFileName(String folder, File image, boolean thumbnail) {
        if (thumbnail) {
            return String.format("%s/thumbnail/%s", folder, image.getName());
        } else {
            return String.format("%s/%s", folder, image.getName());
        }
    }
}
