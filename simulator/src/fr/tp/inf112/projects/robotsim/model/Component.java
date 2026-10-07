package fr.tp.inf112.projects.robotsim.model;

import java.io.Serializable;



import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.canvas.model.Shape;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public abstract class Component implements Figure, Serializable, Runnable {
	
	private static final long serialVersionUID = -5960950869184030220L;

	private String id;

	@JsonBackReference("factory-components")
	private final Factory factory;
	
	private final PositionedShape positionedShape;
	
	private final String name;

	protected Component(final Factory factory,
						final PositionedShape shape,
						final String name) {
		this.factory = factory;
		this.positionedShape = shape;
		this.name = name;

		if (factory != null) {
			factory.addComponent(this);
		}
	}
	
	private static final Logger LOGGER =
		    Logger.getLogger(Component.class.getName());
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PositionedShape getPositionedShape() {
		return positionedShape;
	}
	
	@JsonIgnore
	public Position getPosition() {
		PositionedShape shape =  getPositionedShape();
		return shape == null ? null : shape.getPosition();

	}

	protected Factory getFactory() {
		return factory;
	}

	@JsonIgnore
	@Override
	public int getxCoordinate() {
		PositionedShape shape =  getPositionedShape();
		return shape == null ? -1 : shape.getxCoordinate();
	}

	protected boolean setxCoordinate(int xCoordinate) {
		PositionedShape shape =  getPositionedShape();
		if(shape != null){
			if ( shape.setxCoordinate( xCoordinate ) ) {
				notifyObservers();
				
				return true;
			}
		}
			
			return false;
	}

	@JsonIgnore
	@Override
	public int getyCoordinate() {
		PositionedShape shape =  getPositionedShape();
		return shape == null ? -1 : shape.getyCoordinate();
	}

	protected boolean setyCoordinate(int yCoordinate) {
		PositionedShape shape =  getPositionedShape();
		if(shape != null){
			if ( shape.setyCoordinate( yCoordinate ) ) {
				notifyObservers();
				
				return true;
			}
		}
			
			return false;
	}

	protected void notifyObservers() {
		Factory factory = getFactory();
		if (factory == null)
			return;
		else  factory.notifyObservers();
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [name=" + name + " xCoordinate=" + getxCoordinate() + ", yCoordinate=" + getyCoordinate()
				+ ", shape=" + getPositionedShape();
	}

	@JsonIgnore
	public int getWidth() {
		PositionedShape shape =  getPositionedShape();
		return shape == null ? 0 : shape.getWidth();
	}

	@JsonIgnore
	public int getHeight() {
		PositionedShape shape =  getPositionedShape();
		return shape == null ? 0 : shape.getHeight();
	}
	
	public boolean behave() {
		return false;
	}
	
	@JsonIgnore
	public boolean isMobile() {
		return false;
	}
	
	public boolean overlays(final Component component) {
		if(component == null)
			return false;
		else
			return overlays(component.getPositionedShape());
	}
	
	public boolean overlays(final PositionedShape shape) {

		if (shape == null || getPositionedShape() == null) {

			return false;

		}

		return getPositionedShape().overlays(shape);

	}
	
	public boolean canBeOverlayed(final PositionedShape shape) {
		return false;
	}
	
	@JsonIgnore
	@Override
	public Style getStyle() {
		return ComponentStyle.DEFAULT;
	}
	
	@JsonIgnore
	@Override
	public Shape getShape() {
		return getPositionedShape();
	}
	
	@JsonIgnore
	public boolean isSimulationStarted() {
		Factory factory = getFactory();
		if (factory == null)
			return false;
		else
			return factory.isSimulationStarted();
	}
	
	@Override 
	public void run () {
		while ( isSimulationStarted()) {
			behave();
			
			try{
				Thread.sleep(50);				
			} catch(final InterruptedException ex) {
				LOGGER.log(Level.WARNING, "Component thread was interrupted", ex);
			}
		}
	}
}
