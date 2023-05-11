package ca.waaw.web.rest.utils;

import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyEditorSupport;

@SuppressWarnings("unused")
public class EmptyStringEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        return getValue().toString();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(StringUtils.isEmpty(text) ? null : text);
    }

}
