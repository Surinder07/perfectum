package ca.waaw.web.rest.utils.customannotations.helperclass;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class WhiteSpaceToNullDeserializer  extends StdDeserializer<String> {

    private static final long serialVersionUID = 7527542687158493910L;

    public WhiteSpaceToNullDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return _parseString(p, ctxt).trim().equals("") || _parseString(p, ctxt).trim().equals("null") ?
                null : _parseString(p, ctxt);
    }

}
