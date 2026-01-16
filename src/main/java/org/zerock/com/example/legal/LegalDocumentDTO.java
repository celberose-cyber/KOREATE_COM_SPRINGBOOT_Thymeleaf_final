package org.zerock.com.example.legal;

public class LegalDocumentDTO {
    private long docId;
    private String docType;     // TERMS, PRIVACY
    private String version;
    private String title;
    private String contentMd;
    private String contentHtmlCache;
    private boolean active;

    public long getDocId() { return docId; }
    public void setDocId(long docId) { this.docId = docId; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }

    public String getContentHtmlCache() { return contentHtmlCache; }
    public void setContentHtmlCache(String contentHtmlCache) { this.contentHtmlCache = contentHtmlCache; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
