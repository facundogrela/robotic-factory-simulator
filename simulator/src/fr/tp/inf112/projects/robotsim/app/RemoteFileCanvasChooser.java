package fr.tp.inf112.projects.robotsim.app;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import fr.tp.inf112.projects.robotsim.network.PersistenceClient;
import fr.tp.inf112.projects.robotsim.network.PersistenceRequest;

public class RemoteFileCanvasChooser extends FileCanvasChooser {

    public RemoteFileCanvasChooser(String fileExtension, String documentTypeLabel) {
        super(fileExtension, documentTypeLabel);
    }

    public RemoteFileCanvasChooser(Component viewer, String fileExtension, String documentTypeLabel) {
        super(viewer, fileExtension, documentTypeLabel);
    }

    @Override
    protected String browseCanvases(boolean open) throws IOException {
        if (open) {
            return chooseFileToOpen();
        }
        return chooseSaveFileName();
    }

    private String chooseFileToOpen() throws IOException {
        Object response = PersistenceClient.sendRequest(PersistenceRequest.LIST_FILES);
        if (!(response instanceof String[])) {
            throw new IOException("The server did not return a list of filenames.");
        }
        String[] fileNames = (String[]) response;

        if (fileNames.length == 0) {
            JOptionPane.showMessageDialog(getViewer(),
                "No factories have been saved on the server yet.",
                "Open factory", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return (String) JOptionPane.showInputDialog(
            getViewer(), "Choose a factory saved on the server:", "Open factory",
            JOptionPane.QUESTION_MESSAGE, null, fileNames, fileNames[0]);
    }

    private String chooseSaveFileName() {
        while (true) {
            String name = JOptionPane.showInputDialog(getViewer(), "Enter the factory filename:");
            if (name == null) {
                return null;
            }

            name = name.trim();
            if (!name.isEmpty() && !name.equals(".") && !name.equals("..")
                    && !name.contains("/") && !name.contains("\\")) {
                return name;
            }

            JOptionPane.showMessageDialog(getViewer(),
                "Enter a filename without folders, for example my-factory.",
                "Invalid filename", JOptionPane.WARNING_MESSAGE);
        }
    }
}
