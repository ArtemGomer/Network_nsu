package View;

import Control.GameNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Vector;

public class MainMenu extends JPanel {

    public MainMenu(GameNode node) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JButton newGameButton = new JButton("New game");
        newGameButton.setAlignmentX(CENTER_ALIGNMENT);
        newGameButton.setFocusPainted(false);
        newGameButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                node.stopReceiveAnnouncement();
                node.createNewGame();
            }
        });
        add(newGameButton);
        JCheckBox isViewerBox = new JCheckBox("Viewer");
        isViewerBox.setAlignmentY(CENTER_ALIGNMENT);
        add(isViewerBox);
        Vector<String> columnNames = new Vector<>(Arrays.asList("Name","Size", "Food", "Players", "Can join"));

        JTable table = new JTable(new Vector<Vector<String>>(), columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    System.out.println("CLICKED 2 TIMES");
                    DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
                    String info = (String)tableModel.getValueAt(table.getSelectedRow(), 0);
                    boolean isViewer = isViewerBox.isSelected();
                    node.stopReceiveAnnouncement();
                    node.connectToAGame(isViewer, info);
                }
            }
        });

        add(new JScrollPane(table));
        node.startReceiveAnnouncement((DefaultTableModel)table.getModel());
    }
}
