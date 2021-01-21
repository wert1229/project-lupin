package dto;

public class College {
    private String organName;
    private String query;
    private double jaccard;
    private boolean isNormal;
    private boolean hasDigital;
    private boolean hasOriginal;
    private String digitalUrl;
    private String claimCode;
    private String serviceMethod;
    private String remark;

    public College() {}

    public College(String organName) {
        this.organName = organName;
        this.isNormal = true;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public double getJaccard() {
        return jaccard;
    }

    public void setJaccard(double jaccard) {
        this.jaccard = jaccard;
    }

    public boolean isNormal() {
        return isNormal;
    }

    public void setNormal(boolean normal) {
        isNormal = normal;
    }

    public boolean isHasDigital() {
        return hasDigital;
    }

    public void setHasDigital(boolean hasDigital) {
        this.hasDigital = hasDigital;
    }

    public boolean isHasOriginal() {
        return hasOriginal;
    }

    public void setHasOriginal(boolean hasOriginal) {
        this.hasOriginal = hasOriginal;
    }

    public String getDigitalUrl() {
        return digitalUrl;
    }

    public void setDigitalUrl(String digitalUrl) {
        this.digitalUrl = digitalUrl;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public void setClaimCode(String claimCode) {
        this.claimCode = claimCode;
    }

    public String getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
