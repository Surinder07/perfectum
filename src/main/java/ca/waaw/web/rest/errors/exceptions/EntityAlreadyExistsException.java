package ca.waaw.web.rest.errors.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String entityName;

    private String value;

    public EntityAlreadyExistsException(String entityName, String value) {
        this.entityName = entityName;
        this.value = value;
    }

}
