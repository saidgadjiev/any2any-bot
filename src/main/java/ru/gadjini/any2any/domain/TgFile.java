package ru.gadjini.any2any.domain;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class TgFile {

    public static final String TYPE = "tg_file";

    public static final String FILE_ID = "file_id";

    public static final String FILE_NAME = "file_name";

    public static final String MIME_TYPE = "mime_type";

    public static final String SIZE = "size";

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
        StringBuilder sql = new StringBuilder("(\"");

        sql.append(fileId).append("\",");
        if (StringUtils.isNotBlank(fileName)) {
            sql.append("\"").append(fileName).append("\",");
        } else {
            sql.append(",");
        }
        if (StringUtils.isNotBlank(mimeType)) {
            sql.append("\"").append(mimeType).append("\",");
        } else {
            sql.append(",");
        }
        sql.append(size);
        sql.append(")");

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
