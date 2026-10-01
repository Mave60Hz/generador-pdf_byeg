package com.bizlinks.helpdesk.ui;

import com.bizlinks.helpdesk.config.AppConfig;
import com.bizlinks.helpdesk.service.Db2DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public final class ProdConnectionPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ProdConnectionPanel.class);
    private final AppConfig config;
    private final JButton test = new JButton("Probar conexión PROD");
    private final JLabel status = new JLabel("DB2 PROD: " + AppConfig.PROD_DB2_URL);
    private final JTextArea details = new JTextArea(8, 70);

    public ProdConnectionPanel(AppConfig config) {
        this.config = config;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        JLabel title = new JLabel("Conexión DB2 de producción", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JPanel top = new JPanel(new BorderLayout(8, 14));
        top.add(title, BorderLayout.NORTH);
        top.add(new JLabel("Destino: " + AppConfig.PROD_DB2_URL), BorderLayout.CENTER);
        top.add(test, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setText("La prueba abre y cierra una conexión JDBC. No consulta documentos ni imprime credenciales.\n"
                + "Si hay timeout, verifica VPN/ruta de red y acceso TCP al puerto 52000 del servidor DB2.");
        add(new JScrollPane(details), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
        test.addActionListener(event -> testConnection());
    }

    private void testConnection() {
        test.setEnabled(false);
        status.setText("Conectando a DB2 PROD…");
        details.setText("Intentando abrir conexión a " + AppConfig.PROD_DB2_URL + "\nTiempo límite: 12 segundos.");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return new Db2DocumentRepository(config.primaryDatabase()).testConnection();
            }

            @Override protected void done() {
                try {
                    String database = get();
                    status.setText("Conexión PROD exitosa");
                    details.setText("Conexión establecida correctamente.\n" + database);
                    LOG.info("Prueba de conexión DB2 PROD exitosa: {}", database);
                } catch (Exception error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    status.setText("Falló la conexión PROD");
                    details.setText(cause.getClass().getSimpleName() + ": " + cause.getMessage()
                            + "\n\nSi aparece Connection timed out / SQLSTATE=08001, el cliente no alcanza el host/puerto. Verifica VPN, firewall y ruta a 172.19.35.121:52000.");
                    LOG.error("Falló la prueba de conexión DB2 PROD a {}", AppConfig.PROD_DB2_URL, cause);
                    JOptionPane.showMessageDialog(ProdConnectionPanel.this, details.getText(), "Error de conexión DB2", JOptionPane.ERROR_MESSAGE);
                } finally {
                    test.setEnabled(true);
                }
            }
        }.execute();
    }
}
