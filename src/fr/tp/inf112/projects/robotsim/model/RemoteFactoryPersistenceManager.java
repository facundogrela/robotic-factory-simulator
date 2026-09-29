package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.network.PersistenceClient;

public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {

    public RemoteFactoryPersistenceManager(CanvasChooser canvasChooser) {
        super(canvasChooser);
    }

    @Override
    public Canvas read(String canvasId) throws IOException {
        Object response = PersistenceClient.sendRequest(canvasId);
        if (!(response instanceof Factory)) {
            throw new IOException("The server did not return a factory model.");
        }
        return (Factory) response;
    }

    @Override
    public void persist(Canvas canvasModel) throws IOException {
        if (!(canvasModel instanceof Factory)) {
            throw new IOException("Only factory models can be saved by this manager.");
        }

        Object response = PersistenceClient.sendRequest(canvasModel);
        if (!Boolean.TRUE.equals(response)) {
            throw new IOException("The server did not confirm that the factory was saved.");
        }
    }

    @Override
    public boolean delete(Canvas canvasModel) throws IOException {
        throw new UnsupportedOperationException("Remote deletion is not used by the simulator.");
    }
}
