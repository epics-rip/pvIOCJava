/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;

/**
 * Factory that implements VXI11User.
 * @author mrk
 *
 */
public class VXI11UserFactory {
    /**
     * Create a new instance of VXI11User
     * @return The interface for the new instance.
     */
    public static VXI11User create() {
        return new User();
    }
    private static class User implements VXI11User{
        private boolean bool;
        private byte byteData;
        private byte[] byteArray = null;
        private int intData = 0;
        private String string = null;
        private VXI11ErrorCode error = VXI11ErrorCode.noError;
        private int reason = 0;

        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#clear()
         */
        public void clear() {
            bool = false;
            byteData = 0;
            byteArray = null;
            intData = 0;
            string = null;
            error = VXI11ErrorCode.noError;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getBoolean()
         */
        public boolean getBoolean() {
            return bool;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getByte()
         */
        public byte getByte() {
            return byteData;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getByteArray()
         */
        public byte[] getByteArray() {
            return byteArray;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getError()
         */
        public VXI11ErrorCode getError() {
            return error;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getInt()
         */
        public int getInt() {
            return intData;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getString()
         */
        public String getString() {
            return string;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setBoolean(boolean)
         */
        public void setBoolean(boolean value) {
            bool = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setByte(byte)
         */
        public void setByte(byte value) {
            byteData = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setByteArray(byte[])
         */
        public void setByteArray(byte[] data) {
            byteArray = data;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setError(org.epics.pvioc.pdrv.vxi11.VXI11ErrorCode)
         */
        public void setError(VXI11ErrorCode error) {
            this.error = error;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setInt(int)
         */
        public void setInt(int value) {
            intData = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setString(java.lang.String)
         */
        public void setString(String value) {
            string = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#getReason()
         */
        public int getReason() {
            return reason;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.vxi11.VXI11User#setReason(int)
         */
        public void setReason(int reason) {
            this.reason = reason;
        }
    }
}
