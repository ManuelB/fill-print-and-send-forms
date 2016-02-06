package de.incentergy.letter.sender.cdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

@ApplicationScoped
public class PropertiesConfigurationProducer {

	private static final Logger log = Logger
			.getLogger(PropertiesConfigurationProducer.class.getName());

	private PropertiesConfiguration config;

	@PostConstruct
	public void init() {
		try {
			config = new PropertiesConfiguration(Thread.currentThread()
					.getContextClassLoader().getResource("config.properties"));
			config.setReloadingStrategy(new FileChangedReloadingStrategy());
		} catch (ConfigurationException e) {
			log.log(Level.WARNING, "Could not load config.properties", e);
		}
	}

	@Produces
	public PropertiesConfiguration get() {
		return config;
	}
}
