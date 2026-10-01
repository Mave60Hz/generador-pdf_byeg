package com.bizlinks.helpdesk.ui;

import com.bizlinks.helpdesk.config.AppConfig;
import com.bizlinks.helpdesk.service.HttpXmlValidator;
import com.bizlinks.helpdesk.service.XmlValidator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidationPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationPanel.class);
    private final AppConfig config;
    private final JTextArea xmlArea = new JTextArea();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Código", "Descripción"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JButton validate = new JButton("Validar XML");
    private final JButton export = new JButton("Exportar respuesta");
    private final JButton load = new JButton("Abrir XML");
    private final JLabel status = new JLabel("Abre un XML para validarlo.");
    private final JProgressBar progress = new JProgressBar();
    private Path source;
    private List<XmlValidator.ValidationIssue> issues = List.of();

    public ValidationPanel(AppConfig config) {
        this.config = config;
        setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        xmlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel top = new JPanel(new BorderLayout(8, 0)); top.add(new JLabel("XML de entrada"), BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(load); buttons.add(validate); top.add(buttons, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(xmlArea), new JScrollPane(new JTable(model)));
        split.setResizeWeight(0.58); add(split, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(10, 6)); bottom.add(status, BorderLayout.NORTH);
        progress.setStringPainted(true); bottom.add(progress, BorderLayout.CENTER); bottom.add(export, BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        validate.setEnabled(false); export.setEnabled(false);
        load.addActionListener(e -> chooseXml()); validate.addActionListener(e -> runValidation()); export.addActionListener(e -> saveErrors());
    }

    private void chooseXml() {
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("XML (*.xml)", "xml"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            source = chooser.getSelectedFile().toPath(); xmlArea.setText(Files.readString(source, StandardCharsets.UTF_8));
            validate.setEnabled(!xmlArea.getText().isBlank()); export.setEnabled(false); model.setRowCount(0); issues = List.of();
            status.setText(source.getFileName() + " cargado.");
        } catch (Exception ex) { LOG.error("No se pudo leer el XML", ex); showError("No se pudo leer el XML", ex); }
    }

    private void runValidation() {
        String xml = xmlArea.getText();
        if (xml.isBlank()) { showError("El XML está vacío.", null); return; }
        validate.setEnabled(false); load.setEnabled(false); export.setEnabled(false); progress.setIndeterminate(true); status.setText("Validando…");
        XmlValidator validator = new HttpXmlValidator(config);
        new SwingWorker<List<XmlValidator.ValidationIssue>, Void>() {
            @Override protected List<XmlValidator.ValidationIssue> doInBackground() throws Exception { return validator.validate(xml); }
            @Override protected void done() {
                try {
                    issues = get(); LOG.info("Validación XML terminada: errores={}", issues.size()); model.setRowCount(0);
                    for (var issue : issues) model.addRow(new Object[]{issue.code(), issue.description()});
                    status.setText(issues.isEmpty() ? "El XML no contiene errores." : issues.size() + " error(es) encontrado(s).");
                    export.setEnabled(!issues.isEmpty());
                } catch (Exception ex) { LOG.error("Falló la validación XML", ex); status.setText("No se pudo validar."); showError("Falló la validación", ex.getCause() instanceof Exception cause ? cause : ex); }
                finally { validate.setEnabled(true); load.setEnabled(true); progress.setIndeterminate(false); }
            }
        }.execute();
    }

    private void saveErrors() {
        if (issues.isEmpty()) return;
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Guardar respuesta de validación"); chooser.setSelectedFile(Path.of("ERRORES-" + (source == null ? "documento" : source.getFileName().toString().replaceFirst("(?i)\\.xml$", "")) + ".txt").toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String output = issues.stream().map(issue -> issue.code() + "-" + issue.description()).reduce((a, b) -> a + System.lineSeparator() + b).orElse("");
            Files.writeString(chooser.getSelectedFile().toPath(), output + System.lineSeparator(), StandardCharsets.UTF_8);
            status.setText("Respuesta guardada: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) { LOG.error("No se pudo exportar la respuesta de validación", ex); showError("No se pudo exportar la respuesta", ex); }
    }

    private void showError(String heading, Exception ex) {
        JOptionPane.showMessageDialog(this, ex == null ? heading : heading + ":\n" + ex.getMessage(), "Help Desk", JOptionPane.ERROR_MESSAGE);
    }
}
