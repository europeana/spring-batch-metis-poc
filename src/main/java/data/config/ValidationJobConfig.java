package data.config;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.ValidationItemProcessor;
import data.unit.reader.DefaultRepositoryItemReader;
import data.utility.BatchJobType;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ValidationJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = BatchJobType.VALIDATION.name();
  public static final String STEP_NAME = "validationStep";
  public static final int CHUNK_SIZE = 1;
  public static final int PARALLELIZATION = 2;

  @Bean
  public Job validationBatchJob(JobRepository jobRepository, Step validationStep) {
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(validationStep)
        .build();
  }

  @Bean
  public Step validationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader,
      ItemProcessor<ExecutionRecord, ExecutionRecord> compositeValidationItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      TaskExecutor validationStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, ExecutionRecord>chunk(CHUNK_SIZE, transactionManager)
        .reader(validationRepositoryItemReader)
        .processor(compositeValidationItemProcessor)
        .writer(writer)
        .taskExecutor(validationStepAsyncTaskExecutor)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(CHUNK_SIZE);
    return defaultRepositoryItemReader;
  }

  @Bean
  public ItemProcessor<ExecutionRecord, ExecutionRecord> compositeValidationItemProcessor(
      ValidationItemProcessor validationItemProcessor,
      ItemProcessor<ExecutionRecord, ExecutionRecord> delayLoggingItemProcessor) {
    CompositeItemProcessor<ExecutionRecord, ExecutionRecord> compositeItemProcessor = new CompositeItemProcessor<>();
    compositeItemProcessor.setDelegates(Arrays.asList(validationItemProcessor, delayLoggingItemProcessor));
    return compositeItemProcessor;
  }

  @Bean
  public TaskExecutor validationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(PARALLELIZATION);
    executor.setMaxPoolSize(PARALLELIZATION);
    executor.initialize();
    return executor;
  }

}
