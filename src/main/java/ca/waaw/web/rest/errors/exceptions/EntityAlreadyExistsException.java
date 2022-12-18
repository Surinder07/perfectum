package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String entityType;

    private final String value;

    public EntityAlreadyExistsException(String entityName, String entityType, String value) {
        super();
        this.entityName = entityName;
        this.value = value;
        this.entityType = entityType;
    }

}
