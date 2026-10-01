package com.bizlinks.helpdesk.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class GenerationTableModel extends AbstractTableModel {
    private final String[] columns = {"RUC emisor", "Tipo", "Documento", "ID DB2", "PDF", "UBL", "CDR", "XML data", "Detalle"};
    private final List<GenerationRow> rows = new ArrayList<>();

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int column) { return columns[column]; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        GenerationRow row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.document().issuerTaxId();
            case 1 -> row.document().documentType();
            case 2 -> row.document().documentNumber();
            case 3 -> row.document().id();
            case 4 -> row.pdf();
            case 5 -> row.ubl();
            case 6 -> row.cdr();
            case 7 -> row.xmlData();
            case 8 -> row.detail();
            default -> "";
        };
    }
    public void setRows(List<GenerationRow> values) {
        rows.clear(); rows.addAll(values); fireTableDataChanged();
    }
    public void update(int index, GenerationRow value) { rows.set(index, value); fireTableRowsUpdated(index, index); }
    public List<GenerationRow> rows() { return List.copyOf(rows); }
}
