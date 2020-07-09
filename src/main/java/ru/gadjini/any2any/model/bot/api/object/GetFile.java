package ru.gadjini.any2any.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetFile {

    public static final String METHOD = "downloadfile";

    private static final String FILE_ID = "file_id";

    private static final String REMOVE_PARENT_DIR_ON_CANCEL = "remove_parent_dir_on_cancel";

    @JsonProperty(FILE_ID)
    private String fileId;

    private String path;

    @JsonProperty(REMOVE_PARENT_DIR_ON_CANCEL)
    private boolean removeParentDirOnCancel = false;

    public GetFile() {}

    public GetFile(String fileId, String path, boolean removeParentDirOnCancel) {
        this.fileId = fileId;
        this.path = path;
        this.removeParentDirOnCancel = removeParentDirOnCancel;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isRemoveParentDirOnCancel() {
        return removeParentDirOnCancel;
    }
}
