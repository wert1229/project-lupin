package dto;

public class CentralLib {
    private String organName;
    private String query;
    private double jaccard;
    private boolean isDigital;
    private boolean isOriginal;
    private String digitalUrl;
    private String claimCode;
    private String serviceMethod;
    private String remark;

    public CentralLib(String organName) {
        this.organName = organName;
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

    public boolean isDigital() {
        return isDigital;
    }

    public boolean isOriginal() {
        return isOriginal;
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

    public void setDigital(boolean digital) {
        this.isDigital = digital;
    }

    public void setOriginal(boolean original) {
        this.isOriginal = original;
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
