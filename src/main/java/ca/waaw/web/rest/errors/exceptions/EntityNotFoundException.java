package ca.waaw.web.rest.errors.exceptions;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entity;

    public EntityNotFoundException(String entity) {
        super(entity);
        this.entity = entity;
    }

}
