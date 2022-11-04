package ca.waaw.config;

import ca.waaw.web.rest.utils.customannotations.helperclass.WhiteSpaceToNullDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Configuration
public class GlobalDeserializeConfigurer {

    @Bean
    public Module whiteSpaceToNullDeserializeModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new WhiteSpaceToNullDeserializer());
        return module;
    }

}
