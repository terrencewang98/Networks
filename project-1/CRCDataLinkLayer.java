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
public class CRCDataLinkLayer extends DataLinkLayer {
// =============================================================================

	public boolean testBit(int v, int position){
		return (v & (1 << position)) != 0;
	}

	public int injectBit(int v, int newValue){
		return (v << 1) | newValue;
	}

	public boolean isDivisible(int c){
		return (c &(1<<8)) != 0;
	}

	public String toBitString(final byte[] b){
		final char[] bits = new char[8 * b.length];
    	for(int i = 0; i < b.length; i++) {
        final byte byteval = b[i];
        int bytei = i << 3;
        int mask = 0x1;
        for(int j = 7; j >= 0; j--) {
            final int bitval = byteval & mask;
            if(bitval == 0) {
                bits[bytei + j] = '0';
            } else {
                bits[bytei + j] = '1';
            }
            mask <<= 1;
        }
    }
    return String.valueOf(bits);
	}


    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {
    Queue<Byte> framingData = new LinkedList<Byte>();
	// Begin with the start tag.
	framingData.add(startTag);
	ArrayList<Byte> frame = new ArrayList<Byte>();
	// Add each byte of original data.
	for (int i = 0; i < data.length; i += 1) {

	    // If the current data byte is itself a metadata tag, then precede
	    // it with an escape tag.
	    byte currentByte = data[i];
	    if ((currentByte == startTag) ||
		(currentByte == stopTag) ||
		(currentByte == escapeTag)) {

		framingData.add(escapeTag);
		}

		// Add the data byte itself.
	    framingData.add(currentByte);
		frame.add(currentByte);
	    //if frame is full or last byte
	    if(frame.size() == 8 || i == data.length - 1){
			LinkedList<Integer> bitMessage = new LinkedList<Integer>();
			byte[] byteFrame = new byte[frame.size()];
			for(int j = 0; j < frame.size(); j++){
				byteFrame[j] = frame.get(j);
			}
			frame.clear();
			String bitString = toBitString(byteFrame);
			//add each bit to a linkedlist
			for(int j = 0; j < bitString.length(); j++){
				bitMessage.add(Character.getNumericValue(bitString.charAt(j)));
			}
			//add 8 zeros
			for(int j = 0; j < 8; j++){
				bitMessage.add(0);
			}
			int c = 0;
			while(bitMessage.size() != 0){
				int current = bitMessage.removeFirst();
				c = injectBit(c, current);
				if(isDivisible(c)){
					c = c ^ generator;
				}
			}
			framingData.add(new Integer(c).byteValue());
	    }
	}
	    
	// End with a stop tag.
	framingData.add(stopTag);

	// Convert to the desired byte array.
	byte[] framedData = new byte[framingData.size()];
	Iterator<Byte>  i = framingData.iterator();
	int             j = 0;
	while (i.hasNext()) {
	    framedData[j++] = i.next();
	}
	System.out.println("framedData: " + new String(framedData));
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
	ArrayList<Byte> extractedData = new ArrayList<Byte>();

	while(i.hasNext()){
		byte currentByte = i.next();
		System.out.println("currentByte: " + new String(new byte[] {currentByte}));
		if(frame.size() == 8 || !i.hasNext()){
			LinkedList<Integer> bitMessage = new LinkedList<Integer>();
			//add remainder
			frame.add(currentByte);
			//System.out.println("currentByte: " + new String(new byte[] {currentByte}));
			byte[] byteFrame = new byte[frame.size()];
			for(int j = 0; j < frame.size(); j++){
				byteFrame[j] = frame.get(j);
			}
			String s = toBitString(byteFrame);
			for(int j = 0; j < s.length(); j++){
				bitMessage.add(Character.getNumericValue(s.charAt(j)));
			}
			//division
			int c = 0;
			while(bitMessage.size() != 0){
				int current = bitMessage.removeFirst();
				System.out.println("current: " + current);
				c = injectBit(c, current);
				System.out.println("c: " + c);
				if(isDivisible(c)){
					c = c ^ generator;
					System.out.println("c: " + c);
				}
			}
			if(c != 0){
				byte[] incorrect = new byte[8];
				for(int j = 0; j < frame.size(); j++){
					incorrect[j] = frame.get(j).byteValue();
				}
				System.out.println("Error detected. Incorrect data: " + (new String(incorrect)));
			}
			else{
				for(int j = 0; j < frame.size() - 1; j++){
					extractedData.add(frame.get(j));
					//System.out.println("data: " + new String(extractedData));
				}
			}
			frame.clear();
		}
		else{
			frame.add(currentByte);
		}
	}

	byte[] toReturn = new byte[extractedData.size()];
	for(int j = 0; j < extractedData.size(); j++){
		toReturn[j] = extractedData.get(j);
	}
	return toReturn;

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

    private final int generator = 0b100011101;


    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
    private final byte escapeTag = (byte)'\\';
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
