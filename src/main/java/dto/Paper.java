package dto;

public class Paper {
    private int id;
    private String paperName;
    private CommonInfo info;
    private Congress congress;
    private CentralLib centralLib;
    private Keris keris;
    private College college;

    public Paper(int id, String paperName) {
        this.id = id;
        this.paperName = paperName;
    }

    public Keris getKeris() {
        return keris;
    }

    public void setKeris(Keris keris) {
        this.keris = keris;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public CentralLib getCentralLib() {
        return centralLib;
    }

    public void setCentralLib(CentralLib centralLib) {
        this.centralLib = centralLib;
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }

    public CommonInfo getInfo() {
        return info;
    }

    public void setInfo(CommonInfo info) {
        this.info = info;
    }

    public Congress getCongress() {
        return congress;
    }

    public void setCongress(Congress congress) {
        this.congress = congress;
    }
}
