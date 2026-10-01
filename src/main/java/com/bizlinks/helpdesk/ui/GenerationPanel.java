package com.bizlinks.helpdesk.ui;

import com.bizlinks.helpdesk.config.AppConfig;
import com.bizlinks.helpdesk.model.DocumentInputReader;
import com.bizlinks.helpdesk.model.DocumentRecord;
import com.bizlinks.helpdesk.model.GenerationRow;
import com.bizlinks.helpdesk.model.GenerationTableModel;
import com.bizlinks.helpdesk.service.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenerationPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(GenerationPanel.class);
    private final JTextField inputPath = new JTextField();
    private final JTextField outputPath = new JTextField();
    private final JCheckBox pdf = new JCheckBox("PDF");
    private final JCheckBox ubl = new JCheckBox("UBL");
    private final JCheckBox cdr = new JCheckBox("CDR");
    private final JCheckBox xml = new JCheckBox("XML data");
    private final JButton load = new JButton("Cargar TXT");
    private final JButton output = new JButton("Carpeta salida");
    private final JButton generate = new JButton("Generar archivos");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Carga un TXT para empezar.");
    private final GenerationTableModel tableModel = new GenerationTableModel();
    private final AppConfig config;
    private List<DocumentRecord> documents = List.of();

    public GenerationPanel(AppConfig config) {
        this.config = config;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        inputPath.setEditable(false); outputPath.setEditable(false);
        JPanel inputs = new JPanel(new GridBagLayout());
        inputs.setBorder(BorderFactory.createTitledBorder("Documentos y destino"));
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 6, 5, 6); c.fill = GridBagConstraints.HORIZONTAL;
        addField(inputs, c, 0, "Base DB2 PROD", new JLabel(AppConfig.PROD_DB2_URL), new JLabel(""));
        addField(inputs, c, 1, "Archivo de entrada", inputPath, load);
        addField(inputs, c, 2, "Carpeta de salida", outputPath, output);
        add(inputs, BorderLayout.NORTH);
        JTable table = new JTable(tableModel); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setRowHeight(24);
        table.getColumnModel().getColumn(8).setPreferredWidth(360);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(8, 8));
        JPanel fileTypes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileTypes.setBorder(BorderFactory.createTitledBorder("Archivos a generar"));
        fileTypes.add(pdf); fileTypes.add(ubl); fileTypes.add(cdr); fileTypes.add(xml);
        actions.add(fileTypes, BorderLayout.WEST);
        JPanel bottom = new JPanel(new BorderLayout(10, 0)); bottom.add(status, BorderLayout.NORTH);
        progress.setStringPainted(true); bottom.add(progress, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(generate); bottom.add(buttons, BorderLayout.EAST);
        actions.add(bottom, BorderLayout.SOUTH); add(actions, BorderLayout.SOUTH);
        load.addActionListener(e -> chooseInput()); output.addActionListener(e -> chooseOutput());
        generate.addActionListener(e -> startGeneration()); generate.setEnabled(false);
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, Component value, Component button) {
        c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; panel.add(value, c);
        c.gridx = 2; c.weightx = 0; panel.add(button, c);
    }

    private void chooseInput() {
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            documents = DocumentInputReader.read(chooser.getSelectedFile().toPath());
            LOG.info("Cargados {} documentos desde {}", documents.size(), chooser.getSelectedFile().getAbsolutePath());
            inputPath.setText(chooser.getSelectedFile().getAbsolutePath());
            tableModel.setRows(documents.stream().map(GenerationRow::pending).toList());
            status.setText(documents.size() + " documento(s) cargado(s)."); generate.setEnabled(true);
        } catch (Exception ex) { LOG.error("No se pudo leer el archivo de documentos", ex); showError("No se pudo leer el TXT", ex); }
    }

    private void chooseOutput() {
        JFileChooser chooser = new JFileChooser(); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) outputPath.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void startGeneration() {
        if (documents.isEmpty()) { showError("Carga primero el archivo TXT.", null); return; }
        if (outputPath.getText().isBlank()) { showError("Selecciona una carpeta de salida.", null); return; }
        GenerationOptions options = new GenerationOptions(pdf.isSelected(), ubl.isSelected(), cdr.isSelected(), xml.isSelected());
        if (!options.anySelected()) { showError("Selecciona por lo menos un tipo de archivo.", null); return; }
        Path directory = Path.of(outputPath.getText());
        LOG.info("Iniciando generación: documentos={}, PDF={}, UBL={}, CDR={}, XML data={}, salida={}",
                documents.size(), options.pdf(), options.ubl(), options.cdr(), options.xmlData(), directory);
        generate.setEnabled(false); load.setEnabled(false); output.setEnabled(false);
        progress.setValue(0); progress.setMaximum(documents.size()); progress.setString("0 / " + documents.size());
        DocumentRepository repository = new Db2DocumentRepository(config.primaryDatabase());
        FileGenerationService service = new FileGenerationService(repository, new CompositePdfProvider(config));
        new SwingWorker<List<GenerationRow>, Progress>() {
            @Override protected List<GenerationRow> doInBackground() {
                List<GenerationRow> result = new ArrayList<>();
                for (int i = 0; i < documents.size(); i++) {
                    GenerationRow row = service.generate(documents.get(i), options, directory); result.add(row);
                    publish(new Progress(i, row));
                }
                return result;
            }
            @Override protected void process(List<Progress> chunks) {
                for (Progress update : chunks) tableModel.update(update.index(), update.row());
                int count = chunks.get(chunks.size() - 1).index() + 1;
                progress.setValue(count); progress.setString(count + " / " + documents.size()); status.setText("Procesando documentos…");
            }
            @Override protected void done() {
                try { get(); LOG.info("Generación terminada: documentos={}", documents.size()); status.setText("Proceso terminado. Revisa el estado y detalle por documento."); }
                catch (Exception ex) { LOG.error("Terminó con error inesperado el lote de generación", ex); status.setText("El proceso terminó con un error inesperado."); showError("Falló el proceso de generación", ex); }
                finally { generate.setEnabled(true); load.setEnabled(true); output.setEnabled(true); }
            }
        }.execute();
    }

    private void showError(String heading, Exception ex) {
        String message = ex == null ? heading : heading + ":\n" + ex.getMessage();
        JOptionPane.showMessageDialog(this, message, "Help Desk", JOptionPane.ERROR_MESSAGE);
    }

    private record Progress(int index, GenerationRow row) { }
}
