package rafer;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.io.File;
import java.io.IOException;

public class Swing {

    private static final DataFlavor TEXT = new DataFlavor("text/plain", "text/plain");
    private static final DataFlavor IMAGE = new DataFlavor("image/png", "image/png");
    private static void createAndShowGUI() {
        JFrame frame;

        frame = new JFrame("Rafer");
        frame.setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferHandler.TransferSupport support) {
                System.out.println("test: " + support.isDataFlavorSupported(IMAGE));

                for (DataFlavor data : support.getDataFlavors()) {
                    System.out.println("data " + data.getHumanPresentableName());
                }
                for (DataFlavor data : support.getTransferable().getTransferDataFlavors()) {
                    System.out.println("data " + data.getHumanPresentableName());
                }
                support.setDropAction(COPY);
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                System.out.println("import");

                Transferable t = support.getTransferable();

                try {
                    Object obj = t.getTransferData(IMAGE);
                    System.out.println(obj);
                } catch (UnsupportedFlavorException e) {
                    return false;
                } catch (IOException e) {
                    return false;
                }

                return true;
            }
        });
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