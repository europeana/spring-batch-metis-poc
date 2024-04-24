package data.unit.processor.listener;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class DelayLoggingItemProcessListener implements ItemProcessListener<ExecutionRecord, Future<ExecutionRecordDTO>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public void beforeProcess(@NotNull ExecutionRecord item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull ExecutionRecord executionRecord, Future<ExecutionRecordDTO> executionRecordDTO) {
      LOGGER.info("DelayLoggingItemProcessListener thread: {}", Thread.currentThread());
      LOGGER.info("AfterProcess LOG_DELAY jobId {}, datasetId, executionId, recordId: {}, {}, {}",
              jobInstanceId,
              executionRecord.getExecutionRecordKey().getDatasetId(),
              executionRecord.getExecutionRecordKey().getExecutionId(),
              executionRecord.getExecutionRecordKey().getRecordId());
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public void onProcessError(@NotNull ExecutionRecord executionRecord, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }


}
