package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.tp.inf112.projects.canvas.model.impl.BasicPoint;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

public final class FactoryJsonMapper {

    private FactoryJsonMapper() {
    }

    public static JsonMapper createObjectMapper() {
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("fr.tp.inf112.projects.robotsim.model.")
                .allowIfSubType(BasicPoint.class)
                .allowIfSubType(ArrayList.class)
                .allowIfSubType(LinkedHashSet.class)
                .build();

        return JsonMapper.builder()
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .addMixIn(BasicPoint.class, BasicPointJson.class)
                .activateDefaultTyping(validator, DefaultTyping.NON_FINAL)
                .build();
    }

    public abstract static class BasicPointJson {
        @JsonProperty("xCoordinate")
        public int xCoordinate;

        @JsonProperty("yCoordinate")
        public int yCoordinate;
    }
}
