package com.billFarber.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class MysqlToMarkLogicJobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(MysqlToMarkLogicJobCompletionNotificationListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("BEFORE JOB");
        jobExecution.getExecutionContext().putString("random", "mySqlToMarkLogicJob123");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("AFTER JOB");
    }
}
