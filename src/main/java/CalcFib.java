public class CalcFib {

    private String ppdat;

    private long NBR;

    private long RESULT = 0;

    private long COUNT;

    private long A = 0;

    private long B = 1;

    private String dsp;

    void executeProgram(String ppdat) {
        this.ppdat = ppdat;
        this.NBR = Integer.valueOf(this.ppdat);
        FIB();
        this.dsp = "";
        this.dsp = "FIBONACCI OF: " + this.ppdat + " IS: " + "" + this.RESULT;
        System.out.println(this.dsp);
        this.ppdat = "" + this.RESULT;
    }

    void FIB() {
        if (this.NBR == 1) {
            this.RESULT = 1;
        } else {
            for (this.COUNT = 2; this.COUNT <= this.NBR; this.COUNT++) {
                this.RESULT = this.A + this.B;
                this.A = this.B;
                this.B = this.RESULT;
            }
        }
    }

    public static void main(String[] args) {
        CalcFib instance = new CalcFib();
        instance.executeProgram("20");
    }
}