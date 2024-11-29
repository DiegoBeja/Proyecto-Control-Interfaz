import javax.swing.*;
import com.fazecast.jSerialComm.SerialPort;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;  


public class Interfaz extends JFrame {
    private JTextField anguloInput;
    private JButton enviarButton;
    private JPanel MainInterfaz;
    private JLabel validadorAngulo;
    private JLabel instrucciones;
    private JComboBox comboBox1;
    private JButton conectarButton;
    private JLabel titulo;
    private JPanel panelGrafica;
    private SerialPort sp;
    private XYChart chart;
    private List<Double> tiempos;
    private List<Double> valores;
    private long startTime;

    public Interfaz(){
        setContentPane(MainInterfaz);
        setTitle("Control de posición");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 350);
        setLocationRelativeTo(null);
        setVisible(true);

        validadorAngulo.setVisible(false);
        conectarButton.setEnabled(false);
        enviarButton.setEnabled(false);
        anguloInput.setBorder(null);

        comboBox1.addItem(" ");
        SerialPort[] puertosDisponibles = SerialPort.getCommPorts();
        for(SerialPort sp : puertosDisponibles) {
            comboBox1.addItem(sp.getSystemPortName());
        }

        comboBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(comboBox1.getSelectedIndex() != 0){
                    conectarButton.setEnabled(true);
                } else{
                    conectarButton.setEnabled(false);
                }
            }
        });

        conectarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String puertoSeleccionado = comboBox1.getSelectedItem().toString();
                sp = SerialPort.getCommPort(puertoSeleccionado);
                sp.setComPortParameters(9600, 8, 1, 0);
                sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

                if (sp.openPort()) {
                    enviarButton.setEnabled(true);
                    leerDatosSerial();
                }
            }
        });

        enviarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int angulo = Integer.parseInt(anguloInput.getText());

                if(angulo > 0) {
                    System.out.println(angulo);
                    validadorAngulo.setVisible(false);
                    try {
                        String anguloTexto = anguloInput.getText();
                        sp.getOutputStream().write(anguloTexto.getBytes());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else{
                    validadorAngulo.setVisible(true);
                }
            }
        });

        tiempos = new ArrayList<>();
        valores = new ArrayList<>();
        startTime = System.currentTimeMillis();
        panelGrafica.setLayout(new BorderLayout());
        chart = new XYChartBuilder()
                .width(600).height(300)
                .title("Datos del Arduino")
                .xAxisTitle("Tiempo (s)")
                .yAxisTitle("Valores")
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
        panelGrafica.add(chartPanel, BorderLayout.CENTER);
        panelGrafica.revalidate();
        panelGrafica.repaint();

//        panelGrafica.setLayout(new BorderLayout());
//        double[] x = {1,2,3,4,5};
//        double[] y = {1, 2, 3, 4, 5};
//        chart = QuickChart.getChart("Gráfico de ejemplo", "Tiempo", "Grados", null, x, y);
//        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
//        panelGrafica.add(chartPanel, BorderLayout.CENTER);
//        panelGrafica.revalidate();
//        panelGrafica.repaint();
    }

    private void leerDatosSerial() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[64];
                while (sp != null && sp.isOpen()) {
                    if (sp.bytesAvailable() > 0) {
                        int bytesLeidos = sp.getInputStream().read(buffer);
                        if (bytesLeidos > 0) {
                            String mensaje = new String(buffer, 0, bytesLeidos).trim();
                            try {
                                double valor = Double.parseDouble(mensaje);
                                double tiempo = (System.currentTimeMillis() - startTime) / 1000.0;

                                synchronized (this) {
                                    tiempos.add(tiempo);
                                    valores.add(valor);
                                    if (tiempos.size() > 100) { // Mantener un máximo de 100 puntos
                                        tiempos.remove(0);
                                        valores.remove(0);
                                    }
                                }

                                // Actualizar gráfico
                                SwingUtilities.invokeLater(() -> {
                                    chart.updateXYSeries("Datos", tiempos, valores, null);
                                    panelGrafica.revalidate();
                                    panelGrafica.repaint();
                                });
                            } catch (NumberFormatException e) {
                                System.out.println("Dato no válido recibido: " + mensaje);
                            }
                        }
                    }
                    Thread.sleep(10); // Evitar uso intensivo del CPU
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Error en la lectura serial: " + e.getMessage());
            }
        }).start();
    }

}
