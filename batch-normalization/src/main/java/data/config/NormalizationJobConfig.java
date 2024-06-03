package data.config;

import static data.job.BatchJobType.NORMALIZATION;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.job.incrementer.TimestampJobParametersIncrementer;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.listener.LoggingItemProcessListener;
import data.unit.reader.DefaultRepositoryItemReader;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableTask
public class NormalizationJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = NORMALIZATION.name();
  public static final String STEP_NAME = "normalizationStep";

  @Value("${normalization.chunkSize}")
  public int chunkSize;
  @Value("${normalization.parallelizationSize}")
  public int parallelization;

  @Bean
  public Job normalizationBatchJob(JobRepository jobRepository, Step normalizationStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB, jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .start(normalizationStep)
        .build();
  }

  @Bean
  public Step normalizationStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> normalizationRepositoryItemReader,
      ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> normalizationAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(normalizationRepositoryItemReader)
        .processor(normalizationAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> normalizationRepositoryItemReader(
      ExecutionRecordRepository<ExecutionRecord> executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, chunkSize);
  }

  @Bean
  public TaskExecutor normalizationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(NORMALIZATION.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }

  @Bean
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> normalizationAsyncItemProcessor(
      ItemProcessor<ExecutionRecord, ExecutionRecordDTO> normalizationItemProcessor,
      @Qualifier("normalizationStepAsyncTaskExecutor") TaskExecutor normalizationStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(normalizationItemProcessor);
    asyncItemProcessor.setTaskExecutor(normalizationStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }
}
