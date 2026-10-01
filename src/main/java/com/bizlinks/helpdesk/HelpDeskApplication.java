package com.bizlinks.helpdesk;

import com.bizlinks.helpdesk.config.AppConfig;
import com.bizlinks.helpdesk.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HelpDeskApplication {
    private static final Logger LOG = LoggerFactory.getLogger(HelpDeskApplication.class);
    private HelpDeskApplication() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use Swing's default look and feel if the system one is unavailable.
                LOG.debug("No se pudo aplicar el tema visual del sistema", ignored);
            }
            LOG.info("Iniciando Help Desk Next");
            new MainFrame(AppConfig.fromEnvironment()).setVisible(true);
        });
    }
}
