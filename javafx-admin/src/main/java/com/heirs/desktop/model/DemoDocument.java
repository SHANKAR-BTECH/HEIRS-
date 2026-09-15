package com.heirs.desktop.model;

/**
 * In-memory model representing a supporting document in the HEIRS repository.
 */
public class DemoDocument {

    private final String fileName;
    private final String fileType;
    private final String fileSize;
    private final String updatedDate;
    private final String relatedReference;
    private final String status;

    public DemoDocument(String fileName, String fileType, String fileSize,
                        String updatedDate, String relatedReference, String status) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.updatedDate = updatedDate;
        this.relatedReference = relatedReference;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public String getStatus() {
        return status;
    }
}
