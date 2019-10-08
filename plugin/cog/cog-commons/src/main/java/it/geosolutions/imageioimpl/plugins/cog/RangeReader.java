/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    http://java.net/projects/imageio-ext/
 *    (C) 2019, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageioimpl.plugins.cog;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author joshfix
 * Created on 2019-08-21
 */
public abstract class RangeReader {

    protected URI uri;
    protected ByteBuffer buffer;
    protected int filesize = -1;
    protected int headerLength = 16384;

    private final static Logger LOGGER = Logger.getLogger(RangeReader.class.getName());

    public RangeReader(URI uri) {
        this.uri = uri;
    }

    /**
     * Prevents making new range requests for image data that overlap with the header range that has already been read
     *
     * @param ranges
     * @return
     */
    protected long[][] reconcileRanges(long[][] ranges) {
        boolean modified = false;
        List<long[]> newRanges = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i][0] < headerLength - 1) {
                // this range starts inside of what we already read for the header
                modified = true;
                if (ranges[i][1] <= headerLength - 1) {
                    // this range is fully inside the header which was already read; discard this range
                    LOGGER.fine("Removed range " + ranges[i][0] + "-" + ranges[i][1] + " as it lies fully within"
                            + " the data already read in the header request");
                } else {
                    // this range starts inside the header range, but ends outside of it.
                    // add a new range that starts at the end of the header range
                    long[] newRange = new long[]{headerLength - 1, ranges[i][1]};

                    newRanges.add(newRange);
                    LOGGER.fine("Modified range " + ranges[i][0] + "-" + ranges[i][1]
                            + " to " + headerLength + "-" + ranges[i][1] + " as it overlaps with data previously"
                            + " read in the header request");
                }
            } else {
                // fully outside the header area, keep the range
                newRanges.add(ranges[i]);
            }
        }

        if (modified) {
            return newRanges.toArray(new long[][]{});
        } else {
            LOGGER.fine("No ranges modified.");
            return ranges;
        }
    }

    public void setFilesize(int filesize) {
        buffer = ByteBuffer.allocate(filesize);
        this.filesize = filesize;
    }

    public int getFilesize() {
        return filesize;
    }

    public byte[] getBytes() {
        return buffer.array();
    }

    public void setHeaderLength(int headerLength) {
        this.headerLength = headerLength;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public abstract void readAsync(long[]... ranges);
    public abstract void readAsync(Collection<long[]> ranges);
    public abstract byte[] readHeader();

}
