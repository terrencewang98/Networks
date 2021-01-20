// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
// =============================================================================


// =============================================================================
/**
 * @file   DumbDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

	Queue<Byte> framingData = new LinkedList<Byte>();
	framingData.add(startTag);
	int count = 0;
	int frameCount = 0;
	// Add each byte of original data.
	for (int i = 0; i < data.length; i += 1) {
		//read in the next byte
	    byte currentByte = data[i];

	    // If the current data byte is itself a metadata tag, then precede
	    // it with an escape tag.
	    if ((currentByte == startTag) || (currentByte == stopTag) || (currentByte == escapeTag)) {
			framingData.add(escapeTag);
		}
	    framingData.add(currentByte);
	    count += Integer.bitCount(currentByte);
	    frameCount++;
	    //if frame is full or last byte
	    if(frameCount == 8 || i == data.length - 1){
	    	//even number of 1s
	    	if(count % 2 == 0){
	    		framingData.add((byte)0);
	    	}
	    	else{
	    		framingData.add((byte)1);
	    	}
	    	count = 0;
	    	frameCount = 0;
	    }
	}
	framingData.add(stopTag);

	// Convert to the desired byte array.
	byte[] framedData = new byte[framingData.size()];
	Iterator<Byte>  i = framingData.iterator();
	int             j = 0;
	while (i.hasNext()) {
	    framedData[j++] = i.next();
	}

	return framedData;
	
    } // createFrame ()
    // =========================================================================

   
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected byte[] processFrame () {

	// Search for a start tag.  Discard anything prior to it.
	boolean        startTagFound = false;
	Iterator<Byte>             i = byteBuffer.iterator();
	while (!startTagFound && i.hasNext()) {
	    byte current = i.next();
	    if (current != startTag) {
		i.remove();
	    } else {
		startTagFound = true;
	    }
	}

	// If there is no start tag, then there is no frame.
	if (!startTagFound) {
	    return null;
	}

	// Try to extract data while waiting for an unescaped stop tag.
	Queue<Byte> extractedBytes = new LinkedList<Byte>();
	boolean       stopTagFound = false;
	while (!stopTagFound && i.hasNext()) {

	    // Grab the next byte.  If it is...
	    //   (a) An escape tag: Skip over it and grab what follows as
	    //                      literal data.
	    //   (b) A stop tag:    Remove all processed bytes from the buffer and
	    //                      end extraction.
	    //   (c) A start tag:   All that precedes is damaged, so remove it
	    //                      from the buffer and restart extraction.
	    //   (d) Otherwise:     Take it as literal data.
	    byte current = i.next();
	    if (current == escapeTag) {
		if (i.hasNext()) {
		    current = i.next();
		    extractedBytes.add(current);
		} else {
		    // An escape was the last byte available, so this is not a
		    // complete frame.
		    return null;
		}
	    } else if (current == stopTag) {
		cleanBufferUpTo(i);
		stopTagFound = true;
	    } else if (current == startTag) {
		cleanBufferUpTo(i);
		extractedBytes = new LinkedList<Byte>();
	    } else {
		extractedBytes.add(current);
	    }

	}

	// If there is no stop tag, then the frame is incomplete.
	if (!stopTagFound) {
	    return null;
	}
	
	ArrayList<Byte> frame = new ArrayList<Byte>();
	i = extractedBytes.iterator();
	byte[] extractedData = new byte[extractedBytes.size() - 2];
	int index = 0;
	int count = 0;

	while(i.hasNext()){
		byte current = i.next();
		//System.out.println("current: " + new String(new byte[] {current}));
		//if current byte is a parity byte OR current byte is last byte
		if(frame.size() == 8 || !i.hasNext()){
			//error detected
			if((byte)count % 2 != current){
				byte[] incorrect = new byte[8];
				for(int j = 0; j < frame.size(); j++){
					incorrect[j] = frame.get(j).byteValue();
				}
				System.out.println("Error detected. Incorrect data: " + (new String(incorrect)));
			}
			else{
				for(int j = 0; j < frame.size(); j++){
					extractedData[index] = frame.get(j);
					//System.out.println("data: " + new String(extractedData));
					index++;
				}
			}
			frame.clear();
			count = 0;
		}
		else{
			frame.add(current);
			count += Integer.bitCount(current);
		}
	}

	return extractedData;

    } // processFrame ()
    // ===============================================================



    // ===============================================================
    private void cleanBufferUpTo (Iterator<Byte> end) {

	Iterator<Byte> i = byteBuffer.iterator();
	while (i.hasNext() && i != end) {
	    i.next();
	    i.remove();
	}

    }
    // ===============================================================



    // ===============================================================
    // DATA MEMBERS
    // ===============================================================



    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
    private final byte escapeTag = (byte)'\\';
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
