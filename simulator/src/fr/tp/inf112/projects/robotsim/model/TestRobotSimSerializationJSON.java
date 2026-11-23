package fr.tp.inf112.projects.robotsim.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import fr.tp.inf112.projects.canvas.model.impl.BasicPoint;
import fr.tp.inf112.projects.robotsim.model.path.AbstractFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.JGraphTDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.BasicPolygonShape;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;
import tools.jackson.databind.ObjectMapper;

public class TestRobotSimSerializationJSON {

    private static final Logger LOGGER = Logger.getLogger(TestRobotSimSerializationJSON.class.getName());

    private final ObjectMapper objectMapper = FactoryJsonMapper.createObjectMapper();

    @Test
    public void testSerialization() {
        Factory original = createFactory();

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(original);
        LOGGER.info("Factory as JSON:\n" + json);

        Factory restored = objectMapper.readValue(json, Factory.class);
        checkFactory(original, restored);
        LOGGER.info("PASS: geometry, component types, batteries and shared references.");
    }

    public static Factory createFactory() {
        Factory factory = new Factory(200, 200, "JSON Test Factory", 5);
        factory.setId("json-test.factory");
        Room room = new Room(factory, new RectangularShape(20, 20, 75, 75), "Room");
        new Door(room, Room.WALL.BOTTOM, 10, 20, true, "Door");
        Area area = new Area(room, new RectangularShape(35, 35, 50, 50), "Area");
        Machine machine = new Machine(area, new RectangularShape(50, 50, 15, 15), "Machine");
        new ChargingStation(factory, new RectangularShape(150, 145, 15, 15), "Charger");

        BasicPolygonShape polygon = new BasicPolygonShape();
        polygon.addPoint(new BasicPoint(10, 150));
        polygon.addPoint(new BasicPoint(30, 150));
        polygon.addPoint(new BasicPoint(30, 180));
        polygon.addPoint(new BasicPoint(10, 180));
        new Conveyor(factory, polygon, "Conveyor");
        new Puck(factory, new CircularShape(100, 100, 3), "Puck");

        Battery battery = new Battery(10);
        battery.consume(3);
        JGraphTDijkstraFactoryPathFinder pathFinder = new JGraphTDijkstraFactoryPathFinder(factory, 5);
        Robot robot = new Robot(factory, pathFinder, new CircularShape(5, 5, 2), battery, "Robot 1");
        robot.addTargetComponent(machine);
        Robot second = new Robot(factory, pathFinder, new CircularShape(15, 5, 2), new Battery(20), "Robot 2");
        second.addTargetComponent(machine);
        Robot third = new Robot(factory, new CustomDijkstraFactoryPathFinder(factory, 5),
                new CircularShape(45, 5, 2), new Battery(15), "Robot 3");
        third.addTargetComponent(machine);
        return factory;
    }

    private static void checkFactory(Factory original, Factory restored) {
        assertTrue(restored != original, "A new factory object was created");
        assertTrue(original.getId().equals(restored.getId()), "Factory ID");
        assertTrue(original.getName().equals(restored.getName()), "Factory name");
        assertTrue(restored.getWidth() == 200 && restored.getHeight() == 200, "Factory dimensions");
        assertTrue(restored.getPathResolution() == 5, "Path resolution");
        assertTrue(restored.getComponents().size() == original.getComponents().size(), "Component count");

        for (int i = 0; i < original.getComponents().size(); i++) {
            Component before = original.getComponents().get(i);
            Component after = restored.getComponents().get(i);
            assertTrue(before.getClass() == after.getClass(), "Component type: " + before.getName());
            assertTrue(before.getName().equals(after.getName()), "Component name");
            assertTrue(before.getxCoordinate() == after.getxCoordinate()
                    && before.getyCoordinate() == after.getyCoordinate(), "Position: " + before.getName());
            assertTrue(before.getWidth() == after.getWidth() && before.getHeight() == after.getHeight(),
                    "Dimensions: " + before.getName() + " expected " + before.getWidth() + "x"
                            + before.getHeight() + ", got " + after.getWidth() + "x" + after.getHeight());
            assertTrue(after.getFactory() == restored, "Factory reference: " + before.getName());
        }

        Room room = (Room) restored.getComponents().get(0);
        Door door = (Door) restored.getComponents().get(1);
        Area area = (Area) restored.getComponents().get(2);
        Machine machine = (Machine) restored.getComponents().get(3);
        assertTrue(room.getDoors().get(0) == door, "Shared door");
        assertTrue(room.getAreas().get(0) == area, "Shared area");
        assertTrue(area.getMachine() == machine, "Shared machine");
        assertTrue(door.isOpen(), "Open door state");
        assertTrue(room.getRightWall().getxCoordinate() == 95
                && room.getBottomWall().getyCoordinate() == 95, "Restored wall positions");

        Robot robot = (Robot) restored.getComponents().get(7);
        Robot second = (Robot) restored.getComponents().get(8);
        Robot third = (Robot) restored.getComponents().get(9);
        assertTrue(robot.getBattery().getCapacity() == 10 && robot.getBattery().getLevel() == 7,
                "Battery capacity and remaining charge");
        assertTrue(robot.getTargetComponents().get(0) == machine, "Robot target refers to the same machine");
        assertTrue(second.getTargetComponents().get(0) == machine, "Shared robot target");
        assertTrue(robot.getPathFinder() == second.getPathFinder(), "Shared pathfinder");
        assertTrue(robot.getPathFinder() instanceof JGraphTDijkstraFactoryPathFinder, "JGraphT pathfinder type");
        assertTrue(third.getPathFinder() instanceof CustomDijkstraFactoryPathFinder, "Custom pathfinder type");
        assertTrue(((AbstractFactoryPathFinder<?, ?>) robot.getPathFinder()).getFactoryModel() == restored,
                "Pathfinder references the restored factory");
    }

}
