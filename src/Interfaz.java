import javax.swing.*;
import com.fazecast.jSerialComm.SerialPort;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.jfree.chart.ChartFactory;   // Para crear gráficos
import org.jfree.chart.ChartPanel;    // Para mostrar gráficos en un panel
import org.jfree.chart.JFreeChart;    // Clase principal de gráficos
import org.jfree.data.category.DefaultCategoryDataset; // Para datos categóricos


public class Interfaz extends JFrame {
    private JTextField anguloInput;
    private JButton enviarButton;
    private JPanel MainInterfaz;
    private JLabel validadorAngulo;
    private JLabel instrucciones;
    private JComboBox comboBox1;
    private JButton conectarButton;
    private JLabel titulo;
    private SerialPort sp;
    private JFreeChart grafico;

    public Interfaz(){
        setContentPane(MainInterfaz);
        setTitle("Control de posición");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 300);
        setLocationRelativeTo(null);
        setVisible(true);
        MainInterfaz.setLayout(null);
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
