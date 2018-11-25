import javaScada.Diagramm.Diagramm;
import javaScada.Diagramm.PlotData;
import javaScada.Modbus.Device;
import javaScada.Modbus.ModbusRTU;
import javaScada.Modbus.VarFloat;
import javaScada.Modbus.VarInteger;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class Main {
    JFrame window;
    ModbusRTU ac4;
    Diagramm plotArea;
    Container pane;
    double[] temperature = new double[1];
    double[] counts = new double[1];
    PlotData pd;
    int tlenmax = 24000;
    boolean ready = false;
    File log;
    PrintStream logStream;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        log = new File("log.txt");
        if (log.exists()) log.delete();
        try {
            log.createNewFile();
        } catch (IOException e) {
            System.exit(36);
        }
        try {
            logStream = new PrintStream(log);
        } catch (FileNotFoundException e) {
            System.exit(40);
        }
        logStream.println("log opened");
        logStream.println("creating device");
        Device pr200 = new Device(15);
        try {
            logStream.println("adding variables");
            pr200.addVariable("T", new VarFloat(512, false, true));
            pr200.addVariable("SET", new VarFloat(514, false, true));
            pr200.addVariable("Q", new VarFloat(516, false, true));
            pr200.addVariable("Wh", new VarFloat(518, false, true));
            pr200.addVariable("Ws", new VarFloat(520, false, true));
            pr200.addVariable("Pe", new VarFloat(522, false, true));
            pr200.addVariable("Wr", new VarFloat(524, false, true));
            pr200.addVariable("TPred", new VarFloat(526, false, true));
            pr200.addVariable("IsMan", new VarInteger(528, false));
            pr200.addVariable("Mid", new VarFloat(529, false, true));
        } catch (Exception e) {
            logStream.println("error adding variables");
            e.printStackTrace(logStream);
            exit(1);
        }
        try {
            logStream.println("open com port");
            ac4 = new ModbusRTU("COM6", 19200, 8, 1, 0);
            logStream.println("add device");
            ac4.addDevice("Controller", pr200);
            ready = true;
        } catch (SerialPortException e) {
            System.out.println("problem adding device");
            e.printStackTrace(logStream);
            exit(1);
        }
        logStream.println("open window");
        window = new JFrame("Hello grafiki");
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                try {
                    logStream.println("closing port");
                    ac4.closePort();
                } catch (SerialPortException e) {
                    logStream.println("problem closing port");
                    e.printStackTrace(logStream);
                }
                exit(0);
            }
        });
        pane = window.getContentPane();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point centre = ge.getCenterPoint();
        Diagramm d = new Diagramm();
        d.setPreferredSize(new Dimension(800, 600));
        GroupLayout gl = new GroupLayout(pane);
        window.setLayout(gl);
        gl.setVerticalGroup(gl.createParallelGroup().addComponent(d));
        gl.setHorizontalGroup(gl.createSequentialGroup().addComponent(d));
        window.pack();
        window.setLocation(
                (int) centre.getX() - window.getWidth() / 2,
                (int) centre.getY() - window.getHeight() / 2);
        window.setVisible(true);
        while (true) {
            try {
                Thread.sleep(3600);
            } catch (InterruptedException e) {
            }
            ready = false;
            try {
                ac4.pollDevice("Controller");
                ready = true;
            } catch (SerialPortException e) {
                logStream.println("problem polling divice");
                e.printStackTrace(logStream);
            }
            if (ready) {
                int tlen = temperature.length;
                if (tlen == 1) {
                    temperature[0] = (float) pr200.getVars().get("T").getData();
                    counts[0] = 0;
                }
                if (tlen > tlenmax) {
                    double[] newTemperature = new double[tlenmax + 1];
                    System.arraycopy(temperature, 1, newTemperature, 0, tlenmax);
                    newTemperature[tlenmax] = (float) pr200.getVars().get("T").getData();
                    temperature = newTemperature;
                } else {
                    double[] newTemperature = new double[tlen + 1];
                    System.arraycopy(temperature, 0, newTemperature, 0, tlen);
                    newTemperature[tlen] = (float) pr200.getVars().get("T").getData();
                    temperature = newTemperature;
                    double[] newCounts = new double[tlen + 1];
                    System.arraycopy(counts, 0, newCounts, 0, tlen);
                    newCounts[tlen] = tlen;
                    counts = newCounts;
                }
                d.setPlot(new PlotData(counts, temperature));
                window.repaint();
            }
        }
    }

    public void exit(int n) {
        logStream.close();
        System.exit(n);
    }
}

