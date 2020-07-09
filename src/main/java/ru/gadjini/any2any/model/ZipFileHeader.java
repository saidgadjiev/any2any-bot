package ru.gadjini.any2any.model;

public class ZipFileHeader {

    private String path;

    private long size;

    public ZipFileHeader(String path, long size) {
        this.path = path;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }
}
