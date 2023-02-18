package ca.waaw.web.rest.errors.exceptions.application;

import lombok.Getter;

@Getter
public class PastValueNotDeletableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entityType;

    public PastValueNotDeletableException(String entityType) {
        super();
        this.entityType = entityType;
    }

}
