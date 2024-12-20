import javax.swing.*;
import com.fazecast.jSerialComm.SerialPort;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.knowm.xchart.*;

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
    private JTextField kpInput;
    private JTextField kiInput;
    private JTextField kdInput;
    private JPanel panelPID;
    private JLabel kp;
    private JLabel ki;
    private JLabel kd;
    private JButton resetPID;
    private SerialPort sp;
    private XYChart chart;
    private List<Double> tiempos;
    private List<Double> valores;
    private long startTime;

    public Interfaz(){
        setContentPane(MainInterfaz);
        setTitle("Control de posición");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 400);
        setLocationRelativeTo(null);
        setVisible(true);

        validadorAngulo.setVisible(false);
        conectarButton.setEnabled(false);
        enviarButton.setEnabled(false);
        anguloInput.setBorder(null);
        kpInput.setBorder(null);
        kiInput.setBorder(null);
        kdInput.setBorder(null);
        kpInput.setText("0.6");
        kiInput.setText("0.003");
        kdInput.setText("0.8");
        panelPID.setBackground(new Color(45,42,46));
        kp.setForeground(new Color(168,167,167));
        ki.setForeground(new Color(168,167,167));
        kd.setForeground(new Color(168,167,167));

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
                .xAxisTitle("Tiempo (s)")
                .yAxisTitle("Grados")
                .build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
        panelGrafica.add(chartPanel, BorderLayout.CENTER);
        panelGrafica.revalidate();
        panelGrafica.repaint();

        resetPID.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                kpInput.setText("0.6");
                kiInput.setText("0.003");
                kdInput.setText("0.8");
            }
        });
    }

    private void leerDatosSerial() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[64];
                boolean serieCreada = false; // Bandera para crear la serie solo una vez
                while (sp != null && sp.isOpen()) {
                    if (sp.bytesAvailable() > 0) {
                        int bytesLeidos = sp.getInputStream().read(buffer);
                        if (bytesLeidos > 0) {
                            String mensaje = new String(buffer, 0, bytesLeidos).trim();
                            System.out.println("Mensaje recibido: " + mensaje);  // Depuración adicional

                            // Verificar que el mensaje contenga una coma
                            if (mensaje.contains(",")) {
                                try {
                                    // Dividir el mensaje por coma (ajustado para tu formato)
                                    String[] partes = mensaje.split(",");
                                    if (partes.length == 1) {
                                        // Obtener el valor de anguloActual (primer valor)
                                        double anguloActual = Double.parseDouble(partes[0].trim()); // Asegurarse que sea decimal

                                        double tiempo = (System.currentTimeMillis() - startTime) / 1000.0; // Tiempo en segundos
                                        System.out.println("Tiempo: " + tiempo + ", AnguloActual: " + anguloActual);  // Depuración adicional

                                        // Agregar el valor de anguloActual al eje Y
                                        synchronized (this) {
                                            tiempos.add(tiempo);  // Tiempo en el eje X
                                            valores.add(anguloActual);  // AnguloActual en el eje Y

                                            // Mantener un máximo de 100 puntos en el gráfico
                                            if (tiempos.size() > 100) {
                                                tiempos.remove(0);
                                                valores.remove(0);
                                            }
                                        }

                                        // Crear la serie solo si no se ha creado previamente
                                        if (!serieCreada) {
                                            // Crear la serie "Datos" con los primeros valores
                                            chart.addSeries("Angulo Actual", tiempos, valores);
                                            serieCreada = true;
                                            System.out.println("Serie creada.");
                                        }

                                        // Actualizar el gráfico con los nuevos datos
                                        SwingUtilities.invokeLater(() -> {
                                            System.out.println("Actualizando gráfico...");
                                            chart.updateXYSeries("Angulo Actual", tiempos, valores, null);
                                            panelGrafica.revalidate();
                                            panelGrafica.repaint();
                                            System.out.println("Gráfico actualizado.");
                                        });
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("Error de formato en los datos: " + mensaje);
                                }
                            } else {
                                System.out.println("Formato de mensaje incorrecto (sin coma): " + mensaje);
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
