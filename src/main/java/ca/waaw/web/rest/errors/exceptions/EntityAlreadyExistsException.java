package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String value;

    public EntityAlreadyExistsException(String entityName, String value) {
        super();
        this.entityName = entityName;
        this.value = value;
    }

}
