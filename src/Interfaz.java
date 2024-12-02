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
                .title("Datos del Arduino")
                .xAxisTitle("Tiempo (s)")
                .yAxisTitle("Grados")
                .build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
        panelGrafica.add(chartPanel, BorderLayout.CENTER);
        panelGrafica.revalidate();
        panelGrafica.repaint();
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
                            try {
                                // Dividir el mensaje en base a la coma
                                String[] partes = mensaje.split(" ");
                                if (partes.length == 1) {
                                    // Intentar convertir ambas partes a double
                                    double tiempo = (System.currentTimeMillis() - startTime) / 1000.0; // Tiempo en segundos
                                    double valorY = Double.parseDouble(partes[0].trim()); // Parte después de la coma

                                    // Agregar los valores al eje X y Y
                                    synchronized (this) {
                                        tiempos.add(tiempo); // Tiempo en el eje X
                                        valores.add(valorY); // Valor del sensor en el eje Y

                                        // Mantener un máximo de 100 puntos en el gráfico
                                        if (tiempos.size() > 100) {
                                            tiempos.remove(0);
                                            valores.remove(0);
                                        }
                                    }

                                    // Crear la serie solo si no se ha creado previamente
                                    if (!serieCreada) {
                                        // Crear la serie "Datos" con los primeros valores
                                        chart.addSeries("Datos", tiempos, valores);
                                        serieCreada = true;
                                    }

                                    // Actualizar el gráfico con los nuevos datos
                                    SwingUtilities.invokeLater(() -> {
                                        chart.updateXYSeries("Datos", tiempos, valores, null);
                                        panelGrafica.revalidate();
                                        panelGrafica.repaint();
                                    });
                                }
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
