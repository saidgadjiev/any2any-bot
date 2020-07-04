package ru.gadjini.any2any.domain;

import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class TgFile {

    public static final String TYPE = "tg_file";

    private String fileId;

    private String fileName;

    private String mimeType;

    private int size;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String sql() {
        StringBuilder sql = new StringBuilder();

        sql.append(fileId).append(",");
        sql.append(fileName).append(",");
        sql.append(mimeType).append(",");
        sql.append(size);

        return sql.toString();
    }

    public PGobject sqlObject() {
        PGobject pGobject = new PGobject();
        pGobject.setType(TYPE);
        try {
            pGobject.setValue(sql());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return pGobject;
    }
}
