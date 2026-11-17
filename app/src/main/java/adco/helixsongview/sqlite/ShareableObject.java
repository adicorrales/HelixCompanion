package adco.helixsongview.sqlite;

public class ShareableObject {
    private String filename = "";
    private String folder = "";
    private String title = "";
    private String lastModified = "";
    private String uuid = "";

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}
