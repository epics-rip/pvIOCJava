/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * interface that must be implemented by record support.
 * @author mrk
 *
 */
public interface RecordSupport {
    /**
     * initialize.
     * two passes (0 and 1) are made.
     * During pass 0 the record/link support can initialize the record instance itself but
     * can not try to link to other records.
     * during pass 1 it can comnplete initialization.
     * @param pass 0 or 1
     */
    void initialize(int pass);
    /**
     * clean up any internal state
     */
    void destroy();
    /**
     * perform record processing.
     * @param recordProcess the RecordProcess that called process.
     * @return the result of processing.
     */
    ProcessReturn process(RecordProcess recordProcess);
}
