package ru.gadjini.any2any.model;

public class ZipFileHeader {

    private String path;

    private long size;

    public ZipFileHeader() {
    }

    public ZipFileHeader(String path, long size) {
        this.path = path;
        this.size = size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }
}
