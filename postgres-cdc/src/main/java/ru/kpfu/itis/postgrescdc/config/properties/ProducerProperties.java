package ru.kpfu.itis.postgrescdc.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("pulsar.producer")
public class ProducerProperties {
    private String url;
}
