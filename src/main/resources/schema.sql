CREATE TABLE IF NOT EXISTS EXECUTION_RECORD
(
    DATASET_ID varchar(20),
    EXECUTION_ID varchar(50),
    EXECUTION_NAME varchar(50),
    RECORD_ID varchar(200),
    RECORD_DATA text,
    PRIMARY KEY (DATASET_ID, EXECUTION_ID, RECORD_ID)
);