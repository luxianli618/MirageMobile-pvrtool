/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2002 - 2010 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package flex.messaging.io.amf;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This class represents an already serialized "pass thru" value, whose bytes
 * need to be passed "as is" to the output stream.
 *
 * <p>The scenario that drove this need is to process message return values
 * from non-Java method implementations (e.g .Net), that have already been
 * serialized on the non-Java side.</p>
 */
public class SerializedObject implements Externalizable
{
    protected byte[] objectBytes;
    protected int offset;
    
    /**
     * Constructor. 
     * Construct a SerializedObject with specified object bytes

     * @param objectBytes the actual bytes to write to the output stream.
     */
    public SerializedObject(byte[] objectBytes)
    {
        this(objectBytes, 0);
    }

    /**
     * Constructor. 
     * Construct a SerializedObject with specified object bytes and offset
     * @param objectBytes the actual bytes to write to the output stream.
     * @param offset the offset into the byte array from which to start writing.
     */
    public SerializedObject(byte[] objectBytes, int offset)
    {
        this.objectBytes = objectBytes;
        this.offset = offset;
    }

    /**
     * Get the object bytes.
     * @return the object bytes being held.
     */
    public byte[] getObjectBytes()
    {
        return objectBytes;
    }

    /**
     * Not supported. Serialized objects are intended to be "write only" values.
     * @param in the ObjectInput object
     * @throws IOException, ClassNotFoundException if the read failed
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        throw new UnsupportedOperationException("serialized values can only be written, not read.");
    }

    /**
     * Writes the object bytes directly to the output stream.
     * @param out the ObjectOutput object
     * @throws IOException when the write failed
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        /**
         * So far that the pass through is not working as it will mess up the AMF format
         * We yet to find a viable solution for pass through to work
         * throw Exception to provent the useage of pass through
        for(int i = offset; i < objectBytes.length; i++)
        {
            byte b = objectBytes[i];
            out.writeByte(b);
        }
        */
        throw new UnsupportedOperationException("Pass through does not work, throw exception to provent us using the mechanism.");
    }
}
