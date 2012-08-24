/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;

import java.io.Writer;

/**
 * Interface for tracing pdrv requests.
 * @author mrk
 *
 */
public interface Trace {
    /**
     * An error message.
     */
    public static final int ERROR = 0x0001;
    /**
     * A support message. Normally associated with a particular User.
     */
    public static final int SUPPORT = 0x0002;
    /**
     * A port or device driver message.
     */
    public static final int DRIVER = 0x0008;
    /**
     * A flow of control message.
     */
    public static final int FLOW = 0x0010;
    
    /**
     * printIO requests will not print data.
     */
    public static final int IO_NODATA = 0x0000;
    /**
     * printIO requests will print data in ascii.
     */
    public static final int IO_ASCII = 0x0001;
    /**
     * printIO request will print data in ascii but control characters will be
     * printed as escape sequences.
     */
    public static final int IO_ESCAPE = 0x0002;
    /**
     * printIO will display data as hex.
     */
    public static final int IO_HEX = 0x0004;
    
    /**
     * Set the traceMask.
     * @param mask The mask.
     * It must be some combination of ERROR|SUPPORT|DRIVER
     */
    void setMask(int mask);
    /**
     * Get the trace mask.
     * @return The mask.
     */
    int getMask();
    /**
     * Set the IO trace mask.
     * @param mask The mask.
     * It must be some combination of IO_NODATA|IO_ASCII|IO_ESCAPE|IO_HEX
     */
    void setIOMask(int mask);
    /**
     * Get the IO trace mask.
     * @return The mask.
     */
    int getIOMask();
    /**
     * Set the trace field.
     * @param file The Writer interface.
     */
    void setFile(Writer file);
    /**
     * Get the trace file.
     * @return The Writer interface.
     */
    Writer getFile();
    /**
     * Set the size for truncating data output for printIO requests.
     * @param size The size in bytes.
     */
    void setIOTruncateSize(int size);
    /**
     * Get the size for truncating data output for printIO requests.
     * @return The size in bytes.
     */
    int getIOTruncateSize();
    /**
     * Add a listener for put of any trace option.
     * @param user The user.
     * @param traceOptionChangeListener The listener.
     * @return Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status optionChangeListenerAdd(User user,TraceOptionChangeListener traceOptionChangeListener);
    /**
     * Remove a listener for put of any trace option.
     * @param user The user.
     */
    void optionChangeListenerRemove(User user);
    /**
     * Generate a trace message.
     * @param reason One of ERROR|SUPPORT|DRIVER|FLOW.
     * @param message The message to print
     */
    void print(int reason,String message);
    /**
     * Generate a trace message.
     * @param reason One of ERROR|SUPPORT|DRIVER|FLOW.
     * @param format A format.
     * @param args The data associated with the format.
     */
    void print(int reason,String format, Object... args);
    /**
     * Generate a trace message and also show data values.
     * @param reason One of ERROR|SUPPORT|DRIVER|FLOW.
     * @param buffer The data array.
     * @param length The number of data items.
     * @param message The message to print.
     */
    void printIO(int reason, byte[] buffer,long length,String message);
    /**
     * Generate a trace message and also show data values.
     * @param reason One of ERROR|SUPPORT|DRIVER|FLOW.
     * @param buffer The data array.
     * @param length The number of data items.
     * @param format A format.
     * @param args The data associated with the format.
     */
    void printIO(int reason, byte[] buffer,long length,String format,Object... args);
}
