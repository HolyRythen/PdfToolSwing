import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;

public class PdfToolSwing extends JFrame {
    private final DefaultListModel<File> listModel = new DefaultListModel<>();
    private final JList<File> fileList = new JList<>(listModel);
    private final JButton addBtn = new JButton("Hinzufügen…");
    private final JButton removeBtn = new JButton("Entfernen");
    private final JButton upBtn = new JButton("↑");
    private final JButton downBtn = new JButton("↓");
    private final JButton clearBtn = new JButton("Leeren");
    private final JButton mergeBtn = new JButton("Mergen →");
    private final JButton splitBtn = new JButton("Splitten");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Bereit.");
    private final JCheckBox normalizeNames = new JCheckBox("Dateinamen säubern (für Split)", true);

    public PdfToolSwing() {
        super("PDF Tool (Swing) – Merge & Split (PDFBox)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); SwingUtilities.updateComponentTreeUI(this);} catch (Exception ignored) {}

        fileList.setCellRenderer(new PdfCellRenderer());
        fileList.setVisibleRowCount(12);
        fileList.setDragEnabled(true);
        fileList.setDropMode(DropMode.INSERT);
        enableReorderByDnD();

        // Buttons-Leiste links
        JPanel listBtns = new JPanel(new GridLayout(5,1,6,6));
        listBtns.add(addBtn);
        listBtns.add(removeBtn);
        listBtns.add(upBtn);
        listBtns.add(downBtn);
        listBtns.add(clearBtn);

        JPanel listPanel = new JPanel(new BorderLayout(8,8));
        listPanel.add(new JLabel("PDF-Dateien (Reihenfolge = Merge-Reihenfolge)"), BorderLayout.NORTH);
        listPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        JPanel centerBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        centerBtns.add(mergeBtn);
        centerBtns.add(splitBtn);
        centerBtns.add(normalizeNames);

        JPanel south = new JPanel(new BorderLayout(8,8));
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(240, 22));
        south.add(centerBtns, BorderLayout.WEST);
        JPanel stat = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        stat.add(progress); stat.add(status);
        south.add(stat, BorderLayout.EAST);

        add(listPanel, BorderLayout.CENTER);
        add(listBtns, BorderLayout.WEST);
        add(south, BorderLayout.SOUTH);

        // Aktionen
        addBtn.addActionListener(e -> onAdd());
        removeBtn.addActionListener(e -> onRemove());
        upBtn.addActionListener(e -> moveSelected(-1));
        downBtn.addActionListener(e -> moveSelected(+1));
        clearBtn.addActionListener(e -> { listModel.clear(); setStatus("Liste geleert."); });

        mergeBtn.addActionListener(e -> onMerge());
        splitBtn.addActionListener(e -> onSplit());

        // Drag&Drop direkt auf Liste (Dateien)
        new DropTarget(fileList, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        addPdfFiles(files);
                    }
                } catch (Exception ignored) {}
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void onAdd() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            addPdfFiles(Arrays.asList(fc.getSelectedFiles()));
        }
    }
    private void addPdfFiles(List<File> files) {
        int added = 0;
        for (File f : files) {
            if (f != null && f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                listModel.addElement(f.getAbsoluteFile());
                added++;
            }
        }
        setStatus(added > 0 ? added + " Datei(en) hinzugefügt." : "Keine PDFs erkannt.");
    }

    private void onRemove() {
        List<File> sel = fileList.getSelectedValuesList();
        if (sel.isEmpty()) { msg("Bitte Elemente auswählen."); return; }
        sel.forEach(listModel::removeElement);
        setStatus(sel.size() + " entfernt.");
    }

    private void moveSelected(int dir) {
        int idx = fileList.getSelectedIndex();
        if (idx < 0) return;
        int newIdx = idx + dir;
        if (newIdx < 0 || newIdx >= listModel.size()) return;
        File f = listModel.get(idx);
        listModel.remove(idx);
        listModel.add(newIdx, f);
        fileList.setSelectedIndex(newIdx);
        fileList.ensureIndexIsVisible(newIdx);
    }

    private void onMerge() {
        if (listModel.isEmpty()) { msg("Bitte PDFs zur Liste hinzufügen."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Zieldatei wählen (gemergt)");
        fc.setSelectedFile(new File("merged.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }

        final File dest = out;
        lockUi(true, "Mergen…");
        new Thread(() -> {
            try {
                PDFMergerUtility util = new PDFMergerUtility();
                for (int i=0;i<listModel.size();i++) {
                    util.addSource(listModel.get(i));
                }
                util.setDestinationFileName(dest.getAbsolutePath());
                util.mergeDocuments(null); // PDFBox 3.x: MemoryUsageSetting optional = null
                SwingUtilities.invokeLater(() -> {
                    setStatus("Merge fertig: " + dest.getName());
                    msg("Erfolg!\nGespeichert: " + dest.getAbsolutePath());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> msg("Merge-Fehler: " + ex.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> lockUi(false, "Bereit."));
            }
        }).start();
    }

    private void onSplit() {
        // Modus wählen: Einzelseiten ODER Bereich
        Object[] opts = {"Einzelseiten (alle)", "Bereich(e)…", "Abbrechen"};
        int ch = JOptionPane.showOptionDialog(this, "Wie möchtest du splitten?", "Split",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
        if (ch == 2 || ch == JOptionPane.CLOSED_OPTION) return;

        File source;
        if (listModel.size() == 0) {
            // Keine Liste → einzelne Datei wählen
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("PDF zum Splitten wählen");
            fc.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            source = fc.getSelectedFile();
        } else if (fileList.getSelectedIndices().length == 1) {
            source = fileList.getSelectedValue();
        } else if (fileList.getSelectedIndices().length == 0 && listModel.size() == 1) {
            source = listModel.get(0);
        } else {
            msg("Bitte genau **eine** Datei aus der Liste auswählen (oder Liste leeren und per Dialog wählen).");
            return;
        }

        JFileChooser dir = new JFileChooser();
        dir.setDialogTitle("Zielordner für gesplittete Seiten");
        dir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (dir.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File outDir = dir.getSelectedFile();

        String base = stripExt(source.getName());
        boolean cleanNames = normalizeNames.isSelected();
        String safeBase = cleanNames ? sanitize(base) : base;

        if (ch == 0) {
            // Einzelseiten
            splitIntoSingles(source, outDir, safeBase);
        } else {
            // Bereiche
            String input = JOptionPane.showInputDialog(this,
                    "Seitenbereich(e) angeben (z.B. 1-3,7,10-12). Erste Seite = 1.",
                    "1-3");
            if (input == null || input.trim().isEmpty()) return;
            List<int[]> ranges = parseRanges(input.trim());
            if (ranges.isEmpty()) { msg("Keine gültigen Bereiche erkannt."); return; }
            extractRanges(source, outDir, safeBase, ranges);
        }
    }

    private void splitIntoSingles(File src, File outDir, String baseName) {
        lockUi(true, "Splitte in Einzelseiten…");
        new Thread(() -> {
            try (PDDocument doc = Loader.loadPDF(src)) {
                Splitter splitter = new Splitter();
                List<PDDocument> pages = splitter.split(doc);
                final int total = pages.size();
                final AtomicBoolean ok = new AtomicBoolean(true);

                for (int i = 0; i < total; i++) {
                    PDDocument p = pages.get(i);
                    int pageNum = i + 1;
                    String fn = baseName + "_p" + new DecimalFormat("000").format(pageNum) + ".pdf";
                    File out = new File(outDir, fn);
                    try {
                        p.save(out);
                    } catch (IOException ex) {
                        ok.set(false);
                        SwingUtilities.invokeLater(() -> msg("Fehler beim Speichern: " + out.getName() + "\n" + ex.getMessage()));
                    } finally {
                        try { p.close(); } catch (IOException ignored) {}
                    }
                    final int prog = i+1;
                    SwingUtilities.invokeLater(() -> {
                        progress.setIndeterminate(false);
                        progress.setMinimum(0);
                        progress.setMaximum(total);
                        progress.setValue(prog);
                        status.setText("Seite " + prog + " / " + total);
                    });
                }
                SwingUtilities.invokeLater(() -> {
                    setStatus(ok.get() ? "Split fertig." : "Split mit Fehlern.");
                    msg("Split abgeschlossen.\nZiel: " + outDir.getAbsolutePath());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> msg("Split-Fehler: " + ex.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> lockUi(false, "Bereit."));
            }
        }).start();
    }

    private void extractRanges(File src, File outDir, String baseName, List<int[]> ranges) {
        lockUi(true, "Extrahiere Bereiche…");
        new Thread(() -> {
            try (PDDocument doc = Loader.loadPDF(src)) {
                int totalPages = doc.getNumberOfPages();
                List<Integer> wanted = expandRanges(ranges, totalPages);
                if (wanted.isEmpty()) {
                    throw new IllegalArgumentException("Bereiche enthalten keine gültigen Seiten.");
                }
                int total = wanted.size();
                int idx = 0;
                for (int pageNum : wanted) {
                    try (PDDocument out = new PDDocument()) {
                        out.addPage(doc.getPage(pageNum - 1)); // 1-basiert -> 0-basiert
                        String fn = baseName + "_p" + new DecimalFormat("000").format(pageNum) + ".pdf";
                        out.save(new File(outDir, fn));
                    }
                    idx++;
                    final int prog = idx;
                    SwingUtilities.invokeLater(() -> {
                        progress.setIndeterminate(false);
                        progress.setMinimum(0);
                        progress.setMaximum(total);
                        progress.setValue(prog);
                        status.setText("Seite " + prog + " / " + total);
                    });
                }
                SwingUtilities.invokeLater(() -> {
                    setStatus("Bereiche extrahiert.");
                    msg("Extraktion abgeschlossen.\nZiel: " + outDir.getAbsolutePath());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> msg("Bereichs-Fehler: " + ex.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> lockUi(false, "Bereit."));
            }
        }).start();
    }

    // ---------- Helpers ----------
    private static String stripExt(String name) {
        int i = name.toLowerCase(Locale.ROOT).lastIndexOf(".pdf");
        return i > 0 ? name.substring(0, i) : name;
    }
    private static String sanitize(String s) {
        String t = s.replaceAll("[^\\p{Alnum}_\\-]+", "_");
        // führende/letzte Unterstriche weg
        t = t.replaceAll("^_+|_+$", "");
        return t.isEmpty() ? "document" : t;
    }

    private static List<int[]> parseRanges(String s) {
        // Muster: 1-3,7,10-12
        List<int[]> out = new ArrayList<>();
        for (String part : s.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            Matcher m = Pattern.compile("^(\\d+)(?:-(\\d+))?$").matcher(t);
            if (!m.matches()) continue;
            int a = Integer.parseInt(m.group(1));
            int b = m.group(2) == null ? a : Integer.parseInt(m.group(2));
            if (a <= 0 || b <= 0) continue;
            if (a > b) { int tmp = a; a = b; b = tmp; }
            out.add(new int[]{a,b});
        }
        return out;
    }

    private static List<Integer> expandRanges(List<int[]> ranges, int totalPages) {
        SortedSet<Integer> set = new TreeSet<>();
        for (int[] r : ranges) {
            for (int i = r[0]; i <= r[1]; i++) {
                if (i >= 1 && i <= totalPages) set.add(i);
            }
        }
        return new ArrayList<>(set);
    }

    private void lockUi(boolean busy, String text) {
        addBtn.setEnabled(!busy);
        removeBtn.setEnabled(!busy);
        upBtn.setEnabled(!busy);
        downBtn.setEnabled(!busy);
        clearBtn.setEnabled(!busy);
        mergeBtn.setEnabled(!busy);
        splitBtn.setEnabled(!busy);
        progress.setIndeterminate(busy);
        status.setText(text);
    }
    private void setStatus(String s) { status.setText(s); }
    private static void msg(String s) { JOptionPane.showMessageDialog(null, s); }

    private void enableReorderByDnD() {
        fileList.setTransferHandler(new FileListReorderHandler());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PdfToolSwing().setVisible(true));
    }

    // --- UI Helpers ---
    private static class PdfCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File f) setText(f.getName());
            return c;
        }
    }

    /** Ermöglicht Drag&Drop-Reihenfolge in der JList. */
    private class FileListReorderHandler extends TransferHandler {
        private int fromIndex = -1;
        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override protected Transferable createTransferable(JComponent c) {
            fromIndex = fileList.getSelectedIndex();
            return new StringSelection(Integer.toString(fromIndex));
        }
        @Override public boolean canImport(TransferSupport s) { return s.isDataFlavorSupported(DataFlavor.stringFlavor); }
        @Override public boolean importData(TransferSupport s) {
            if (!canImport(s)) return false;
            try {
                int toIndex;
                JList.DropLocation dl = (JList.DropLocation) s.getDropLocation();
                toIndex = dl.getIndex();
                String idxStr = (String) s.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int from = Integer.parseInt(idxStr);
                if (from == toIndex || from < 0 || toIndex < 0 || from >= listModel.size()) return false;
                File f = listModel.get(from);
                listModel.remove(from);
                if (toIndex > listModel.size()) toIndex = listModel.size();
                listModel.add(toIndex, f);
                fileList.setSelectedIndex(toIndex);
                return true;
            } catch (Exception ignored) { return false; }
        }
    }
}
