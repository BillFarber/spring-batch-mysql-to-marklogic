import com.billFarber.batch.listener.MysqlToMarkLogicJobCompletionNotificationListener;
import com.billFarber.batch.processor.MysqlToMarklogicProcessor;
import com.billFarber.model.RecordSO;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.ext.helper.DatabaseClientProvider;
import com.marklogic.spring.batch.item.processor.MarkLogicItemProcessor;
import com.marklogic.spring.batch.item.writer.MarkLogicItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.ResultSet;

/**
 * MySqlToMarkLogicJob.java - a Spring Batch configuration template that demonstrates ingesting data into MarkLogic from MySql.
 * This job specification uses the MarkLogicBatchConfiguration that utilizes a MarkLogic implementation of a JobRepository.
 *
 * @author Phil Barber
 * @version 1.4.0
 * @see EnableBatchProcessing
 * @see com.marklogic.spring.batch.config.MarkLogicBatchConfiguration
 * @see com.marklogic.spring.batch.config.MarkLogicConfiguration
 * @see com.marklogic.spring.batch.item.processor.MarkLogicItemProcessor
 * @see com.marklogic.spring.batch.item.writer.MarkLogicItemWriter
 */
@EnableBatchProcessing
@Import(value = {
        com.marklogic.spring.batch.config.MarkLogicBatchConfiguration.class,
        com.marklogic.spring.batch.config.MarkLogicConfiguration.class})
@PropertySource("classpath:job.properties")
public class MySqlToMarkLogicJobConfig {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // This is the bean label for the name of your Job.  Pass this label into the job_id parameter
    // when using the CommandLineJobRunner
    private final String JOB_NAME = "mySqlToMarkLogicJob";

    /**
     * The JobBuilderFactory and Step parameters are injected via the EnableBatchProcessing annotation.
     *
     * @param jobBuilderFactory injected from the @EnableBatchProcessing annotation
     * @param step              injected from the step method in this class
     * @return Job
     */
    @Bean(name = JOB_NAME)
    public Job job(JobBuilderFactory jobBuilderFactory, Step step) {
        JobExecutionListener listener = new MysqlToMarkLogicJobCompletionNotificationListener();

        return jobBuilderFactory.get(JOB_NAME)
                .start(step)
                .listener(listener)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    /**
     * The StepBuilderFactory and DatabaseClientProvider parameters are injected via Spring.  Custom parameters must be annotated with @Value.
     *
     * @param stepBuilderFactory     injected from the @EnableBatchProcessing annotation
     * @param databaseClientProvider injected from the BasicConfig class
     * @param collections            This is an example of how user parameters could be injected via command line or a properties file
     * @return Step
     * @see DatabaseClientProvider
     * @see ItemReader
     * @see ItemProcessor
     * @see DocumentWriteOperation
     * @see MarkLogicItemProcessor
     * @see MarkLogicItemWriter
     */
    @Bean
    @JobScope
    public Step step(
            StepBuilderFactory stepBuilderFactory,
            DatabaseClientProvider databaseClientProvider,
            ItemReader<RecordSO> mysqlReader,
            @Value("#{jobParameters['output_collections'] ?: 'mySqlToMarkLogicJob'}") String[] collections,
            @Value("#{jobParameters['chunk_size'] ?: 20}") int chunkSize) {

        DatabaseClient databaseClient = databaseClientProvider.getDatabaseClient();

        MysqlToMarklogicProcessor mysqlToMarkLogicProcessor = new MysqlToMarklogicProcessor(collections);

        MarkLogicItemWriter writer = new MarkLogicItemWriter(databaseClient);
        writer.setBatchSize(chunkSize);


        ChunkListener chunkListener = new ChunkListener() {

            @Override
            public void beforeChunk(ChunkContext context) {
                logger.info("beforeChunk");
            }

            @Override
            public void afterChunk(ChunkContext context) {
                logger.info("afterChunk");
            }

            @Override
            public void afterChunkError(ChunkContext context) {

            }
        };

        return stepBuilderFactory.get("step1")
                .<RecordSO, DocumentWriteOperation>chunk(chunkSize)
                .reader(mysqlReader)
                .processor(mysqlToMarkLogicProcessor)
                .writer(writer)
                .listener(chunkListener)
                .build();
    }

    @Value("${spring.datasource.driverClassName}")
    private String databaseDriver;
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(databaseDriver);
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(databaseUsername);
        dataSource.setPassword(databasePassword);
        return dataSource;
    }

    @Bean
    public ItemReader<RecordSO> reader(DataSource dataSource) {
        JdbcCursorItemReader<RecordSO> reader = new JdbcCursorItemReader<>();
        reader.setSql("select id, firstName, lastname, random_num from reader");
        reader.setDataSource(dataSource);
        reader.setRowMapper(
                (ResultSet resultSet, int rowNum) -> {
                    if (!(resultSet.isAfterLast()) && !(resultSet.isBeforeFirst())) {
                        RecordSO recordSO = new RecordSO();
                        recordSO.setFirstName(resultSet.getString("firstName"));
                        recordSO.setLastName(resultSet.getString("lastname"));
                        recordSO.setId(resultSet.getInt("Id"));
                        recordSO.setRandomNum(resultSet.getString("random_num"));

                        logger.info("RowMapper record : {}", recordSO);
                        return recordSO;
                    } else {
                        logger.info("Returning null from rowMapper");
                        return null;
                    }
                });
        return reader;
    }

}
