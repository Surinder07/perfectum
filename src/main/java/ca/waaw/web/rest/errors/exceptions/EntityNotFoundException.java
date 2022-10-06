package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entity;

    private String value = null;

    public EntityNotFoundException(String entity) {
        super(entity);
        this.entity = entity;
    }

    public EntityNotFoundException(String entity, String value) {
        super(entity);
        this.entity = entity;
        this.value = value;
    }

}
