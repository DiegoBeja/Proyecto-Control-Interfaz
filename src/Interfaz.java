import javax.swing.*;
import com.fazecast.jSerialComm.SerialPort;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;


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

    public Interfaz(){
        setContentPane(MainInterfaz);
        setTitle("Control de posición");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 350);
        setLocationRelativeTo(null);
        setVisible(true);

        panelGrafica.setLayout(new BorderLayout());
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {1, 2, 3, 4, 5};
        chart = QuickChart.getChart("Gráfico de ejemplo", "X", "Y", null, x, y);
        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
        panelGrafica.add(chartPanel, BorderLayout.CENTER);
        panelGrafica.revalidate();
        panelGrafica.repaint();

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
                    if(sp.openPort()) {
                        enviarButton.setEnabled(true);
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
    }
}
