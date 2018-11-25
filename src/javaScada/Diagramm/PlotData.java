package javaScada.Diagramm;

import java.awt.*;

public class PlotData {
    public Color lineColor = new Color(0, 0, 0);
    public double[] xArray;
    public double[] yArray;
    private int plotStyle;

    public PlotData(double[] xArray, double[] yArray) {
        this.xArray = xArray;
        this.yArray = yArray;
    }

    public PlotData(double[] xArray, double[] yArray, Color c) {
        this.xArray = xArray;
        this.yArray = yArray;
        lineColor = c;
    }

    public double min(double[] array) {
        double min = Double.MAX_VALUE;
        for (double current : array) {
            if (current < min) min = current;
        }
        return min;
    }

    public double max(double[] array) {
        double max = -Double.MAX_VALUE;
        for (double current : array) {
            if (current > max) max = current;
        }
        return max;
    }
}
