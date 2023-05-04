package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityCantBeActivatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entity;

    private final String reasonEntities;

    public EntityCantBeActivatedException(String entity, String reasonEntities) {
        super();
        this.entity = entity;
        this.reasonEntities = reasonEntities;
    }

}
