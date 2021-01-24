package dto;

public class CentralLib {
    private String organName;
    private String query;
    private double similarity;
    private Boolean isDigital;
    private Boolean isOriginal;
    private String digitalUrl;
    private String claimCode;
    private String serviceMethod;
    private String remark;
    private String authorDiff;

    public CentralLib() {}

    public CentralLib(String organName) {
        this.organName = organName;
        this.isDigital = false;
        this.isOriginal = false;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOrganName() {
        return organName;
    }

    public String getQuery() {
        return query;
    }

    public double getSimilarity() {
        return similarity;
    }

    public Boolean getDigital() {
        return isDigital;
    }

    public void setDigital(Boolean digital) {
        isDigital = digital;
    }

    public Boolean getOriginal() {
        return isOriginal;
    }

    public void setOriginal(Boolean original) {
        isOriginal = original;
    }

    public String getDigitalUrl() {
        return digitalUrl;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public String getServiceMethod() {
        return serviceMethod;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public void setDigitalUrl(String digitalUrl) {
        this.digitalUrl = digitalUrl;
    }

    public void setClaimCode(String claimCode) {
        this.claimCode = claimCode;
    }

    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public String getAuthorDiff() {
        return authorDiff;
    }

    public void setAuthorDiff(String authorDiff) {
        this.authorDiff = authorDiff;
    }
}
