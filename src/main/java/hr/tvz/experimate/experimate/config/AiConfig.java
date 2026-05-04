package hr.tvz.experimate.experimate.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring AI configuration — builds the {@link ChatClient} bean with the
 * application-wide default system prompt loaded from {@code ai/system-prompt.txt}.
 */
@Configuration
public class AiConfig {

    /**
     * Builds and exposes the {@link ChatClient} with the global system prompt
     * pre-loaded from the classpath. Individual service methods add task-specific
     * instructions on top of this context.
     *
     * @param builder the Spring AI auto-configured builder
     * @return a ready-to-use {@link ChatClient}
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(new ClassPathResource("ai/system-prompt.txt"))
                .build();
    }
}
