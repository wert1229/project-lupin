package dto;

public class CentralLib {
    private String organName;
    private String query;
    private double jaccard;
    private Boolean isDigital;
    private Boolean isOriginal;
    private String digitalUrl;
    private String claimCode;
    private String serviceMethod;
    private String remark;

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

    public double getJaccard() {
        return jaccard;
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

    public void setJaccard(double jaccard) {
        this.jaccard = jaccard;
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
}
