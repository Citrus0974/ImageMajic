package edu.image.majic.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.adobe.AdobeJpegDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import com.drew.metadata.png.PngDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MetadataUtils {
    public static void printAllMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            System.out.println("--" + metadata);
            for (Directory directory : metadata.getDirectories()) {
                System.out.println("-" + directory);
                for (Tag tag : directory.getTags()) {
                    System.out.println(tag);
                }
            }
        } catch (ImageProcessingException | IOException e) {
            System.out.println("Error processing file " + file.getAbsolutePath() + " - " + e.getMessage());
        }

    }

    public static void printAllExifMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifIFD0Directory != null) {
                for (Tag tag : exifIFD0Directory.getTags()) {
                    System.out.println(tag);
                }
            }
            if (exifSubIFDDirectory != null) {
                for (Tag tag : exifSubIFDDirectory.getTags()) {
                    System.out.println(tag);
                }
            }

        } catch (ImageProcessingException | IOException e) {
            System.out.println("Error processing EXIF of image " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    public static HashMap<String, String> fillImageMetadataHashmap(HashMap<String, String> metadataHashMap, File image) {
        metadataHashMap.put("File name", image.getName());
        if (image == null) {
            return metadataHashMap;
        }
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(image);
            Directory directory = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
            if (directory != null) {
                metadataHashMap.put("File type", directory.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_LONG_NAME) +
                        " (" + directory.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE) + ")");
            }
            directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
            if (directory != null) {
                metadataHashMap.put("File size", directory.getDescription(FileSystemDirectory.TAG_FILE_SIZE));
                metadataHashMap.put("Last modified", directory.getDescription(FileSystemDirectory.TAG_FILE_MODIFIED_DATE));
            }
            directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null) {
                metadataHashMap.put("Camera manufacturer", directory.getDescription(ExifIFD0Directory.TAG_MAKE));
                metadataHashMap.put("Camera model", directory.getDescription(ExifIFD0Directory.TAG_MODEL));
                metadataHashMap.put("Editing software", directory.getDescription(ExifIFD0Directory.TAG_SOFTWARE));
                metadataHashMap.put("Date/Time", directory.getDescription(ExifIFD0Directory.TAG_DATETIME));
            }
            directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                metadataHashMap.put("Exposure Time", directory.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                metadataHashMap.put("F-Number (Aperture)", directory.getDescription(ExifSubIFDDirectory.TAG_FNUMBER) == null ? directory.getDescription(ExifSubIFDDirectory.TAG_APERTURE) : directory.getDescription(ExifSubIFDDirectory.TAG_FNUMBER));
                metadataHashMap.put("ISO Speed", findTagInAllDirectories(metadata, "ISO"));
                metadataHashMap.put("Focal length", directory.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                metadataHashMap.put("Focal length (35-mm equivalent)", directory.getDescription(ExifSubIFDDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH));
                metadataHashMap.put("Color space", directory.getDescription(ExifSubIFDDirectory.TAG_COLOR_SPACE));
                metadataHashMap.put("Lens manufacturer", directory.getDescription(ExifSubIFDDirectory.TAG_LENS_MAKE));
                metadataHashMap.put("Lens model", directory.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL));
                metadataHashMap.put("Lens specification", directory.getDescription(ExifSubIFDDirectory.TAG_LENS_SPECIFICATION));
            }
            directory = metadata.getFirstDirectoryOfType(AdobeJpegDirectory.class);
            if (directory != null) {
                metadataHashMap.put("Color transform", directory.getDescription(AdobeJpegDirectory.TAG_COLOR_TRANSFORM));
            }
            directory = metadata.getFirstDirectoryOfType(PngDirectory.class);
            if (directory != null) {
                metadataHashMap.put("Color transform", directory.getDescription(PngDirectory.TAG_COLOR_TYPE));
            }
            return metadataHashMap;
        } catch (ImageProcessingException | IOException e) {
            System.out.println("Error processing image " + image.getAbsolutePath() + " - " + e.getMessage());
            return metadataHashMap;
        }
    }

    private static String findTagInAllDirectories(Metadata metadata, String targetTagName) {
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if (tag.getTagName().toLowerCase().contains(targetTagName.toLowerCase())) {
                    return tag.getDescription();
                }
            }
        }
        return null;
    }
}
