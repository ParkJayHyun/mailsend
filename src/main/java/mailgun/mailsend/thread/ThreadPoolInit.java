package mailgun.mailsend.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ThreadPoolInit {

    @Bean(name = "executor")
    public Executor setExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(150);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("task-");
        return executor;
    }
}
