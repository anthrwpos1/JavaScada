package javaScada.Diagramm;

public class Axes {
    private static final double[] mantissList = {0.75, 1.0, 1.5, 2, 2.5, 3, 4, 5, 7.5};
    private static double[] logMantissList;

    static {
        logMantissList = new double[mantissList.length];
        for (int i = 0; i < mantissList.length; i++) {
            logMantissList[i] = Math.log10(mantissList[i]);
        }
    }

    public Double xmin, xmax, ymin, ymax;
    private int xcnum = 10;
    private int ycnum = 10;
    private double[] xMarks;
    private double[] yMarks;

    Axes(double xmin, double xmax, double ymin, double ymax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        xMarks = axisMarks(xcnum, xmin, xmax);
        yMarks = axisMarks(ycnum, ymin, ymax);
    }

    private double[] axisMarks(int cnum, double min, double max) {
        double pow = Math.floor(Math.log10((max - min) * 10 / cnum));
        double en = (Math.log10((max - min) * 10 / cnum) - pow);
        double enerr = 2;
        double curerr;
        double mantiss = 0;
        for (int i = 0; i < mantissList.length; i++) {
            if ((curerr = Math.abs(en - logMantissList[i])) < enerr) {
                enerr = curerr;
                mantiss = mantissList[i];
            }
        }
        double delay = Math.pow(10, (pow - 1)) * mantiss;
        double markmin = Math.round(min / delay) * delay;
        double markmax = Math.round(max / delay) * delay;
        int marksnum = (int) Math.round((markmax - markmin) / delay);
        double[] marks = new double[marksnum];
        for (int i = 0; i < marksnum; i++) {
            marks[i] = markmin + delay * (double) i;
        }
        return marks;
    }

    public void refresh(Axes newAxes) {
        if (newAxes.xmin < xmin) xmin = newAxes.xmin;
        if (newAxes.xmax > xmax) xmax = newAxes.xmax;
        if (newAxes.ymin < ymin) ymin = newAxes.ymin;
        if (newAxes.ymax > ymax) ymax = newAxes.ymax;
        xMarks = axisMarks(xcnum, xmin, xmax);
        yMarks = axisMarks(ycnum, ymin, ymax);
    }

    public double[] getxMarks() {
        return xMarks;
    }

    public double[] getyMarks() {
        return yMarks;
    }
}
