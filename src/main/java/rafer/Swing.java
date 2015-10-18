package rafer;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;

public class Swing {
    private static void createAndShowGUI() {
        JFrame frame;
        JTextComponent label;
        DropTargetListener dtl;

        frame = new JFrame("Rafer");
        label = new JTextField("Welcome");
        label.setDragEnabled(true);
        label.setDropMode(DropMode.INSERT);
        frame.getContentPane().add(label);
        dtl = new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                Transferable t;

/*               System.out.println("drop " + dtde + " " + dtde.getDropAction());
                if (dtde.getDropAction() != DnDConstants.ACTION_COPY) {
                    dtde.rejectDrop();
                    return;
                }
                System.out.println(dtde.getCurrentDataFlavorsAsList());*/
                for (Object obj : dtde.getCurrentDataFlavorsAsList()) {
                    System.out.println(obj.toString());
                }
                t = dtde.getTransferable();
                if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    dtde.acceptDrop(1);
                    try {
                        System.out.println("transferable: " + dtde.getTransferable().getTransferData(DataFlavor.stringFlavor));
                    } catch (UnsupportedFlavorException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dtde.dropComplete(true);
                } else {
                    dtde.rejectDrop();
                }
            }
        };
        frame.setDropTarget(new DropTarget(label, dtl));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}