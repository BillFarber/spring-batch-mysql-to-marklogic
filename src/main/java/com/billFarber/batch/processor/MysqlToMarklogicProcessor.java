package com.billFarber.batch.processor;

import com.billFarber.model.RecordSO;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.client.io.marker.DocumentMetadataWriteHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

public class MysqlToMarklogicProcessor implements ItemProcessor<RecordSO, DocumentWriteOperation> {

    private static final Logger logger = LoggerFactory.getLogger(MysqlToMarklogicProcessor.class);

    private String[] collections;

    public MysqlToMarklogicProcessor(String[] collections) {
        this.collections = collections;
    }

    @Override
    public DocumentWriteOperation process(RecordSO item) {
        DocumentWriteOperation dwo = new DocumentWriteOperation() {

            @Override
            public OperationType getOperationType() {
                return OperationType.DOCUMENT_WRITE;
            }

            @Override
            public String getUri() {
                return UUID.randomUUID().toString() + ".json";
            }

            @Override
            public DocumentMetadataWriteHandle getMetadata() {
                DocumentMetadataHandle metadata = new DocumentMetadataHandle();
                metadata.withCollections(collections);
                return metadata;
            }

            @Override
            public AbstractWriteHandle getContent() {
                String itemString = String.format("{ \"name\" : \"%s\" }", item.getFirstName());
                logger.info(itemString);
                return new StringHandle(itemString);
            }

            @Override
            public String getTemporalDocumentURI() {
                return null;
            }
        };
        return dwo;
    }
}

