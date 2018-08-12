package com.example.envault;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
	* exports the contents of certain keys as environment variables. the "keys" - the environment variables - live in
	* the {@link Resource} described by ${code envars.keys.resource}.
	*
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Configuration
@ComponentScan
public class EnvaultApplication {

		@Configuration
		static class VaultConfiguration extends AbstractVaultConfiguration {

				private final Environment environment;

				VaultConfiguration(Environment environment) {
						this.environment = environment;
				}

				@Override
				public VaultEndpoint vaultEndpoint() {
						VaultEndpoint vaultEndpoint = new VaultEndpoint();
						vaultEndpoint.setScheme("http");
						return vaultEndpoint;
				}

				@Override
				public ClientAuthentication clientAuthentication() {
						return new TokenAuthentication(this.environment.getProperty("spring.cloud.vault.token"));
				}
		}

		@Bean
		InitializingBean run(@Value("${envars.keys.resource}") Resource varsTxtFile, VaultTemplate template) {
				return () -> {

						Function<String, String> valueFor = key -> {
								VaultResponseSupport<Map> response = template.read("secret/" + key, Map.class);
								return String.class.cast(response.getData().get("value"));
						};

						List<String> envVarKeys = Files
							.readAllLines(varsTxtFile.getFile().toPath())
							.stream()
							.map(String::trim)
							.filter(x -> !x.isEmpty())
							.collect(Collectors.toList());

						envVarKeys
							.parallelStream()
							.forEach(key -> System.out.println("export " + key + "=" + valueFor.apply(key)));
				};
		}

		public static void main(String[] args) {
				SpringApplication.run(EnvaultApplication.class, args);
		}
}
