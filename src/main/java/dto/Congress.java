package dto;

public class Congress {
    private String organName;
    private Boolean isDigital;
    private Boolean isOriginal;
    private String digitalUrl;
    private String claimCode;
    private String controlCode;
    private String serviceMethod;
    private String remark;

    public Congress(String organName) {
        this.organName = organName;
        this.isDigital = false;
        this.isOriginal = false;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
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

    public String getControlCode() {
        return controlCode;
    }

    public void setControlCode(String controlCode) {
        this.controlCode = controlCode;
    }
}
