package javaScada.Diagramm;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Diagramm extends JPanel {//область построения
    private ArrayList<PlotData> plotDatae = new ArrayList<>();
    private Axes axes;                  //Todo: сделать возможность дополнительных осей
    private boolean autoAxes = true;    //Todo: добавить возможности масштабирования
    private int s = 15;//отступ под оси
    // Todo: назвать нормально переменные

    private Axes getLimits(PlotData pd) {//автоопределение пределов
        return new Axes(
                pd.min(pd.xArray),
                pd.max(pd.xArray),
                pd.min(pd.yArray),
                pd.max(pd.yArray));
    }

    public void setPlot(PlotData pd) {//замена графика
        plotDatae = new ArrayList<>();
        plotDatae.add(pd);
        axes = getLimits(pd);
    }

    public void addPlot(PlotData pd) {//добавление графика
        plotDatae.add(pd);
        axes.refresh(getLimits(pd));
    }

    public void paintComponent(Graphics g) {//отрисовка Todo: добавить сетку
        super.paintComponent(g);            //          Todo: переделать графику через Swing.Graphics2D
        g.clearRect(0, 0, getWidth(), getHeight());
        if (plotDatae != null) {
            for (int i1 = 0; i1 < plotDatae.size(); i1++) {
                PlotData plotData = plotDatae.get(i1);
                g.setColor(plotData.lineColor);
                for (int i = 0; i < (plotData.xArray.length - 1); i++) {
                    int x1 = toScreenXTransform(plotData.xArray[i]);
                    int y1 = toScreenYTransform(plotData.yArray[i]);
                    int x2 = toScreenXTransform(plotData.xArray[i + 1]);
                    int y2 = toScreenYTransform(plotData.yArray[i + 1]);
                    g.drawLine(x1, y1, x2, y2);
                }
                g.setColor(Color.BLACK);
                g.drawLine(getWidth() - s, (getHeight() - s), getWidth() - s - 10, (getHeight() - s) + 5);
                g.drawLine(getWidth() - s, (getHeight() - s), getWidth() - s - 10, (getHeight() - s) - 5);
                g.drawLine(s, (getHeight() - s), getWidth() - s, (getHeight() - s));
                g.drawLine(s, (getHeight() - s), s, s);
                g.drawLine(s, s, s - 5, s + 10);
                g.drawLine(s, s, s + 5, s + 10);
                double[] xmarks = axes.getxMarks();
                double[] ymarks = axes.getyMarks();
                for (int i = 0; i < xmarks.length; i++) {
                    int xmark = toScreenXTransform(xmarks[i]);
                    g.drawLine(xmark, (getHeight() - s - 5), xmark, (getHeight() - s + 5));
                    g.drawString(String.valueOf(xmarks[i]), xmark, (getHeight() - s));//todo: сделать нормальные надписи
                }
                for (int i = 0; i < ymarks.length; i++) {
                    int ymark = toScreenYTransform(ymarks[i]);
                    g.drawLine(s - 5, ymark, s + 5, ymark);
                    g.drawString(String.valueOf(ymarks[i]), s, ymark);
                }
            }
        }
    }

    private int toScreenXTransform(double x) {
        double xmin = axes.xmin;
        double dx = axes.xmax - axes.xmin;
        return (int) ((x - xmin) / dx * (double) (getWidth() - s)) + s;
    }

    private int toScreenYTransform(double y) {
        double ymin = axes.ymin;
        double dy = axes.ymax - axes.ymin;
        return getHeight() - (int) ((y - ymin) / dy * (double) (getHeight() - s)) - s;
    }
}