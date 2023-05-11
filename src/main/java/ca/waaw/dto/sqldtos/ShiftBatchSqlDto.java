package ca.waaw.dto.sqldtos;

import java.time.Instant;

public interface ShiftBatchSqlDto {
    String getId();
    String getBatchId();
    String getStatus();
    String getName();
    Instant getStart();
    Instant getEnd();
    Instant getCreatedDate();
}