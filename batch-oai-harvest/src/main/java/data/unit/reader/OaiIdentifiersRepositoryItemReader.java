package data.unit.reader;

import data.entity.ExecutionRecordExternalIdentifier;
import data.repositories.ExecutionRecordExternalIdentifierRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@StepScope
public class OaiIdentifiersRepositoryItemReader extends RepositoryItemReader<ExecutionRecordExternalIdentifier> {

    @Value("#{stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

    public OaiIdentifiersRepositoryItemReader(ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
        this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setRepository(executionRecordExternalIdentifierRepository);
        setSort(Collections.emptyMap());
        setMethodName("findAllByExecutionRecordKey_ExecutionId");
        setArguments(List.of(jobInstanceId+""));

        super.afterPropertiesSet();
    }
}
