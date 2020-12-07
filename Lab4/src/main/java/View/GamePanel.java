package View;

import Control.GameNode;
import me.ippolitov.fit.snakes.SnakesProto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Vector;

public class GamePanel extends JPanel {

    GameNode node;
    JTable table;
    KeyAdapter adapter;

    public GamePanel(GameNode node) {
        setLayout(null);
        this.node = node;

        JButton exitButton = new JButton("Exit");
        exitButton.setBounds(900, 620, 200, 80);
        exitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                node.stopPlaying();
                node.toMainMenu();
            }
        });
        add(exitButton);

        Vector<String> columnNames = new Vector<>(Arrays.asList("Name", "Score"));

        table = new JTable(new Vector<Vector<String>>(), columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JScrollPane pane = new JScrollPane(table);
        pane.setBounds(900, 20, 300, 600);
        add(pane);

        adapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    node.sendSteerMsg(Direction.DOWN);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    node.sendSteerMsg(Direction.UP);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    node.sendSteerMsg(Direction.RIGHT);
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    node.sendSteerMsg(Direction.LEFT);
                }
            }
        };

        addKeyListener(adapter);

    }

    public JPanel player() {
        addKeyListener(adapter);
        return this;
    }

    public JPanel viewer() {
        if (listenerList.getListenerCount() > 0) {
            removeKeyListener(adapter);
        }
        return this;
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
        tableModel.setRowCount(0);
        Vector<Vector<String>> score = node.getPlayersScore();
        for (Vector<String> strings : score) {
            tableModel.addRow(strings);
        }

        int cellSide = Math.min(800 / node.getFieldWidth(), 800 / node.getFieldHeight());
        for (int i = 0; i < node.getFieldWidth(); i++) {
            for (int j = 0; j < node.getFieldHeight(); j++) {
                switch (node.getCellState(i, j)) {
                    case SNAKE_BODY: {
                        g.setColor(Color.BLACK);
                        g.drawRect(i * cellSide, j * cellSide, cellSide, cellSide);
                        if (node.getId() == node.getCellId(i, j)) {
                            g.setColor(new Color(23, 198, 47));
                        } else {
                            g.setColor(new Color(184, 56, 56));
                        }
                        g.fillRect(i * cellSide + 1, j * cellSide + 1, cellSide - 1, cellSide - 1);
                        break;
                    }
                    case SNAKE_HEAD: {
                        g.setColor(Color.BLACK);
                        g.drawRect(i * cellSide, j * cellSide, cellSide, cellSide);
                        if (node.getId() == node.getCellId(i, j)) {
                            g.setColor(new Color(18, 74, 26));
                        } else {
                            g.setColor(new Color(88, 16, 16));
                        }
                        g.fillRect(i * cellSide + 1, j * cellSide + 1, cellSide - 1, cellSide - 1);
                        break;
                    }
                    case EMPTY: {
                        g.setColor(Color.BLACK);
                        g.drawRect(i * cellSide, j * cellSide, cellSide, cellSide);
                        g.setColor(Color.GRAY);
                        g.fillRect(i * cellSide + 1, j * cellSide + 1, cellSide - 1, cellSide - 1);
                        break;
                    }
                    case FOOD: {
                        g.setColor(Color.BLACK);
                        g.drawRect(i * cellSide, j * cellSide, cellSide, cellSide);
                        g.setColor(Color.BLUE);
                        g.fillRect(i * cellSide + 1, j * cellSide + 1, cellSide - 1, cellSide - 1);
                        break;
                    }
                }
            }
        }
    }
}
