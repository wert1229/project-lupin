package dto;

public class Keris {
    private String organName;
    private String query;
    private double jaccard;
    private Boolean isDigital;
    private String digitalUrl;
    private String serviceMethod;
    private String remark;

    public Keris(String organName) {
        this.organName = organName;
        this.isDigital = false;
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

    public Boolean getDigital() {
        return isDigital;
    }

    public void setDigital(Boolean digital) {
        isDigital = digital;
    }

    public String getDigitalUrl() {
        return digitalUrl;
    }

    public void setDigitalUrl(String digitalUrl) {
        this.digitalUrl = digitalUrl;
    }

    public String getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }
}
