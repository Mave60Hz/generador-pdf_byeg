package com.bizlinks.helpdesk.ui;

import com.bizlinks.helpdesk.config.AppConfig;

import javax.swing.*;
import java.awt.*;

public final class MainFrame extends JFrame {
    public MainFrame(AppConfig config) {
        super("Help Desk | Gestión de documentos electrónicos");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 680)); setSize(1280, 820); setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gestor de archivos", new GenerationPanel(config));
        tabs.addTab("Validar XML", new ValidationPanel(config));
        tabs.addTab("Conexión DB2 PROD", new ProdConnectionPanel(config));
        setContentPane(tabs);
    }
}
