package com.liyh.entity;

public class UploadFileInfo {
    private Integer currentChunk;
    private Integer chunks;
    private String fileName;
    private int uploadedChunks; // 添加这个字段

    public Integer getCurrentChunk() {
        return currentChunk;
    }

    public void setCurrentChunk(Integer currentChunk) {
        this.currentChunk = currentChunk;
    }

    public Integer getChunks() {
        return chunks;
    }

    public void setChunks(Integer chunks) {
        this.chunks = chunks;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getUploadedChunks() {
        return uploadedChunks;
    }

    public void incrementUploadedChunks() {
        this.uploadedChunks++;
    }
}
