package org.poc.objs.assetrepository.spi;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WriteExtensionConfiguration {

    @Bean
    PreprocessingExtension noopPreprocessingExtension() {
        return (context, batch) -> batch;
    }

    @Bean
    EventExtension noopEventExtension() {
        return changes -> List.of();
    }
}
