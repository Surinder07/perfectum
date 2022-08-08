package ca.waaw.web.rest.utils.customannotations.helperclass;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringJoiner;

public class FirstAlphabetCapitalDeserializer  extends StdDeserializer<String> {

    private static final long serialVersionUID = 7527542687158493910L;

    public FirstAlphabetCapitalDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        StringJoiner joiner = new StringJoiner(" ");
        Arrays.stream(_parseString(p, ctxt).toLowerCase().split(" "))
                .map(StringUtils::capitalize)
                .forEach(joiner::add);
        return joiner.toString();
    }

}
