package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityNotDeletableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entity;

    private final String reasonEntities;

    public EntityNotDeletableException(String entity, String reasonEntities) {
        super();
        this.entity = entity;
        this.reasonEntities = reasonEntities;
    }

}
