/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author Chris
 */
public abstract class Slices {

    /**
     * This will duplicate the buffer so that the slice interactions will not
     * affect buffer.
     *
     * @param buffer
     * @return
     */
    public static Slice create(ByteBuffer buffer) {
        return new Wrapper(buffer);
    }

    /**
     * This will duplicate the buffer so that the slice interactions will not
     * affect buffer. This should be slightly more performant than
     * Slices.create(buffer).extract(offset, limit);
     *
     * @param buffer
     * @param offset
     * @param limit
     * @return
     */
    public static Slice create(ByteBuffer buffer, int offset, int limit) {
        return new Wrapper(buffer, offset, limit);
    }

    /**
     * This will duplicate the buffer so that the slice interactions will not
     * affect buffer.
     *
     * @param buffers
     * @return
     */
    public static Slice create(ByteBuffer... buffers) {
        if (buffers.length == 1) {
            return new Wrapper(buffers[0]);
        } else if (buffers.length == 2) {
            return new Pair(new Wrapper(buffers[0]), new Wrapper(buffers[1]));
        } else {
            Slice[] slices = new Slice[buffers.length];
            for (int i = 0; i < slices.length; i++) {
                slices[i] = new Wrapper(buffers[i]);
            }
            return new Chain(slices);
        }
    }

    /**
     * This will chain the slices without duplicating them. If you want to the
     * chain to be independent, then duplicate the slices before chaining.
     *
     * @param a
     * @return
     */
    public static Slice chain(Slice... a) {
        if (a.length == 1) {
            return a[0];
        } else if (a.length == 2) {
            return new Pair(a[0], a[1]);
        } else {
            return new Chain(a);
        }
    }

    //Hoping this is more performant thant Chain for 2 slices
    private static class Pair extends Abstract {

        private final Slice _a;
        private final Slice _b;
        private Slice _current;

        private Pair(Slice a, Slice b, boolean resetPosition) {
            _a = a;
            _b = b;
            _current = _a;
            if (resetPosition) {
                _a.position(0);
                _b.position(0);
            }
        }

        public Pair(Slice a, Slice b) {
            this(a, b, true);
        }

        @Override
        public int getUnsignedByte() {
            if (_current.position() >= _current.limit()) {
                if (_current == _a) {
                    _current = _b;
                }
            }
            return _current.getUnsignedByte();
        }

        @Override
        public int internalPosition() {
            if (_current == _a) {
                return _a.position();
            } else {
                return _b.position() + _a.limit();
            }
        }

        @Override
        public void internalPosition(int position) {
            if (position < _a.limit()) {
                _a.position(position); // must set position before swapping in case position throws an exception
                _b.position(0);
                _current = _a;
            } else {
                _b.position(position - _a.limit());
                _current = _b;
            }
        }

        @Override
        public Slice internalExtract(int start, int length) {
            int end = start + length;
            if (end <= _a.limit()) {
                //just a
                return _a.extract(start, length);
            } else if (start < _a.limit() && end > _a.limit()) {
                //both
                return new Pair(_a.extract(start, _a.limit() - start), _b.extract(end - _a.limit()));
            } else {
                //just b
                return _b.extract(start - _a.limit(), end - start);
            }
        }

        @Override
        public int limit() {
            return _b.limit() + _a.limit();
        }

        @Override
        public Slice duplicate() {
            Slice a = _a.duplicate();
            Slice b = _b.duplicate();
            Pair pair = new Pair(a, b, false);
            if (_current == _a) {
                pair._current = a;
            } else {
                pair._current = b;
            }
            return pair;
        }

        @Override
        public int read(ByteBuffer dst) {
            if (_current == _a) {
                int read = _a.read(dst);
                if (!_a.hasRemaining()) {
                    _current = _b;
                    read += _b.read(dst);
                }
                return read;
            } else {
                return _b.read(dst);
            }
        }

        @Override
        public int read(WritableByteChannel dst) throws IOException {
            int read = _a.read(dst);
            read += _b.read(dst);
            return read;
        }

    }

    private static class Chain extends Abstract {

        private Slice[] _slices;
        private int _currentSlice;
        private int _position;
        private int _limit;

        public Chain(Slice... slices) {
            _slices = slices;
            _limit = -1;
            rewind();
        }

        private void rewind() {
            _position = 0;
            _currentSlice = 0;
            _slices[_currentSlice].position(0);
        }

        @Override
        public int internalPosition() {
            return _position;
        }

        @Override
        public void internalPosition(int position) {
            internalSkip(position - _position);
        }

        public void internalSkip(int delta) {
            Slice current = _slices[_currentSlice];
            int newPos = current.position() + delta;
            if (newPos >= current.limit()) {
                delta = delta - current.remaining();
                _position += current.remaining();
                current.position(current.limit());
                if (checkSlice()) {
                    skip(delta);
                }
            } else if (newPos < 0) {
                if (_currentSlice == 0) {
                    _position -= current.position();
                    current.position(0);
                } else {
                    delta += current.position();
                    _position -= current.position();
                    current.position(0);
                    _currentSlice--;
                    _slices[_currentSlice].position(_slices[_currentSlice].limit());
                    skip(delta);
                }
            } else {
                _position += delta;
                current.position(newPos);
            }
        }

        @Override
        public int limit() {
            if (_limit < 0) {
                _limit = 0;
                for (Slice slice : _slices) {
                    _limit += slice.limit();
                }
            }
            return _limit;
        }

        @Override
        public int getUnsignedByte() {
            int read = _slices[_currentSlice].getUnsignedByte();
            if (read >= 0) {
                _position++;
                return read;
            } else if (checkSlice()) {
                return getUnsignedByte();
            }
            return -1;
        }

        @Override
        public int read(ByteBuffer dst) {
            int startPosition = _position;
            while (dst.hasRemaining() && checkSlice()) {
                int read = _slices[_currentSlice].read(dst);
                if (read > 0) {
                    _position += read;
                }
            }
            int totalRead = _position - startPosition;
            if (totalRead == 0 && !hasRemaining()) {
                totalRead = -1;
            }
            return totalRead;
        }

        @Override
        public int read(WritableByteChannel dst) throws IOException {
            int startPosition = _position;
            while (checkSlice()) {
                int read = _slices[_currentSlice].read(dst);
                if (read > 0) {
                    _position += read;
                }
            }
            return _position - startPosition;
        }

        private boolean checkSlice() {
            if (!_slices[_currentSlice].hasRemaining()) {
                if (_currentSlice < _slices.length - 1) {
                    _currentSlice++;
                    _slices[_currentSlice].position(0);
                } else {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Slice duplicate() {
            Slice[] slices = new Slice[_slices.length];
            for (int i = 0; i < _slices.length; i++) {
                slices[i] = _slices[i].duplicate();
            }
            Slice duplicate = Slices.chain(slices);
            duplicate.position(_position);
            return duplicate;
        }

        @Override
        public Slice internalExtract(int start, int length) {
            int[] startSlice = findIndexAndPosition(start, 0, 0);
            int[] endSlice = findIndexAndPosition(start + length - 1, startSlice[0], start - startSlice[1]);
            return extract(startSlice[0], startSlice[1], endSlice[0], endSlice[1]);
        }

//        @Override
//        public void internalCut(int start, int length) {
//            int[] startSlice = findIndexAndPosition(start, 0, 0);
//            int[] endSlice = findIndexAndPosition(start + length - 1, startSlice[0], start - startSlice[1]);
//            cut(startSlice[0], startSlice[1], endSlice[0], endSlice[1]);
//        }
        //Searches all slices (starting at startIndex) for the specified position
        //startIndexPosition is the global position where the startIndex slice begins
        //Returns {index_of_slice, local_position_in_that_slice}
        private int[] findIndexAndPosition(int position, int startIndex, int startIndexPosition) {
            int pos = startIndexPosition;
            for (int i = startIndex; i < _slices.length; i++) {
                int localPosition = position - pos;
                if (localPosition < _slices[i].limit()) {
                    return new int[]{i, localPosition};
                } else {
                    pos += _slices[i].limit();
                }
            }
            return new int[]{-1, -1};
        }

//        private Slice cut(int startIndex, int startPosition, int endIndex, int endPosition) {
//            if (startIndex < 0 || startPosition < 0) {
//                throw new IllegalArgumentException("Cannot cut slice. Invalid start position.");
//            }
//            if (endIndex < startIndex || endPosition < 0) {
//                throw new IllegalArgumentException("Cannot cut slice. Invalid end position.");
//            }
//            int newSliceCount = endIndex - startIndex + 1;
//            Slice[] newSlices = new Slice[newSliceCount];
//
//            if (startIndex == endIndex) {
//                _slices[startIndex].cut(startPosition, endPosition - startPosition + 1);
//                newSlices[0] = _slices[startIndex];
//            } else {
//                if (startPosition > 0) {
//                    _slices[startIndex].cut(startPosition, _slices[startIndex].limit() - startPosition);
//                }
//                if (endPosition < _slices[endIndex].limit() - 1) {
//                    _slices[endIndex].cut(endPosition + 1);
//                }
//                for (int i = 0; i < newSliceCount; i++) {
//                    newSlices[i] = _slices[i + startIndex];
//                }
//            }
//
//            _slices = newSlices;
//            _limit = -1;
//            rewind();
//            return this;
//        }
        private Slice extract(int startIndex, int startPosition, int endIndex, int endPosition) {
            if (startIndex < 0 || startPosition < 0) {
                throw new IllegalArgumentException("Cannot extract slice. Invalid start position.");
            }
            if (endIndex < startIndex || endPosition < 0) {
                throw new IllegalArgumentException("Cannot extract slice. Invalid end position.");
            }
            int newSliceCount = endIndex - startIndex + 1;
            Slice startSlice = _slices[startIndex];
            if (startIndex == endIndex) {
                Slice newSlice = startSlice.extract(startPosition, endPosition - startPosition + 1);
                return Slices.chain(newSlice);
            } else {
                Slice[] newSlices = new Slice[newSliceCount];
                //start
                if (startPosition > 0) {
                    newSlices[0] = startSlice.extract(startPosition, startSlice.limit() - startPosition);
                } else {
                    newSlices[0] = startSlice.duplicate();
                }
                //middle
                for (int i = 1; i < newSliceCount - 1; i++) {
                    newSlices[i] = _slices[i + startIndex].duplicate();
                }
                //end
                Slice endSlice = _slices[endIndex];
                if (endPosition < endSlice.limit() - 1) {
                    newSlices[newSliceCount - 1] = endSlice.extract(endPosition + 1);
                } else {
                    newSlices[newSliceCount - 1] = endSlice.duplicate();
                }
                return Slices.chain(newSlices);
            }
        }

    }

    private static class Wrapper extends Abstract {

        private ByteBuffer _buffer;

        private Wrapper(ByteBuffer buffer, boolean resetPosition) {
            _buffer = buffer.duplicate();
            if (resetPosition) {
                _buffer.position(0);
            }
        }

        public Wrapper(ByteBuffer buffer, int offset, int limit) {
            int position = buffer.position();
            buffer.position(offset);
            _buffer = buffer.slice();
            _buffer.limit(limit - offset);
            buffer.position(position);
        }

        //For optimization of extract
        //Creates a Wrapper from buffer's current position for length
        private Wrapper(ByteBuffer buffer, int length) {
            _buffer = buffer.slice();
            _buffer.limit(length);
        }

        public Wrapper(ByteBuffer buffer) {
            this(buffer, true);
        }

        @Override
        public Slice duplicate() {
            return new Wrapper(_buffer, false);
        }

        @Override
        protected int internalPosition() {
            return _buffer.position();
        }

        @Override
        protected void internalPosition(int position) {
            _buffer.position(position);
        }

        @Override
        public int limit() {
            return _buffer.limit();
        }

        @Override
        public int getUnsignedByte() {
            if (position() < limit()) {
                return (0xFF) & _buffer.get();
            } else {
                return -1;
            }
        }

        @Override
        protected Slice internalExtract(int start, int length) {
            return new Wrapper(_buffer, start, start + length);
        }

        //Optimization
        @Override
        public Slice extract(int length) {
            return new Wrapper(_buffer, length);
        }

//        @Override
//        public void internalCut(int start, int length) {
//            _buffer.position(start);
//            _buffer.limit(start + length);
//            _buffer = _buffer.slice();
//        }
//
//        @Override
//        public void cut(int length) {
//            if (length <= 0) {
//                throw new IllegalArgumentException("length must be greater than zero");
//            }
//            _buffer.limit(length);
//        }
        @Override
        public int read(ByteBuffer dst) {
            int srcSize = _buffer.remaining();
            int read = Math.min(dst.remaining(), srcSize);
            byte[] b = new byte[read];
            _buffer.get(b);
            dst.put(b);
            if (read == 0 && srcSize == 0) {
                read = -1;
            }
            return read;
        }

        @Override
        public int read(WritableByteChannel dst) throws IOException {
            return dst.write(_buffer);
        }
    }

    private static abstract class Abstract implements Slice {

        public Abstract() {
        }

        protected abstract int internalPosition();

        protected abstract void internalPosition(int position);

        protected abstract Slice internalExtract(int start, int length);

//        protected abstract void internalCut(int start, int length);
        @Override
        public void skip(int delta) {
            position(position() + delta);
        }

        @Override
        public int position() {
            return internalPosition();
        }

        @Override
        public void position(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("position cannot be negative");
            } else if (position > limit()) {
                throw new IllegalArgumentException("position cannot exceed limit");
            }
            internalPosition(position);
        }

        @Override
        public Slice extract(int start, int length) {
            if (start < 0) {
                throw new IllegalArgumentException("start cannot be negative");
            }
            if (start + length > limit()) {
                throw new IllegalArgumentException("end cannot exceed limit");
            }
            if (length <= 0) {
                throw new IllegalArgumentException("length must be greater than zero");
            }
            return internalExtract(start, length);
        }

        @Override
        public Slice extract(int length) {
            return extract(position(), length);
        }

//        @Override
//        public void cut(int start, int length) {
//            if (start < 0) {
//                throw new IllegalArgumentException("start cannot be negative");
//            }
//            if (start + length > limit()) {
//                throw new IllegalArgumentException("end cannot exceed limit");
//            }
//            if (length <= 0) {
//                throw new IllegalArgumentException("length must be greater than zero");
//            }
//            internalCut(start, length);
//        }
//
//        @Override
//        public void cut(int length) {
//            cut(position(), length);
//        }
        @Override
        public int remaining() {
            return limit() - position();
        }

        @Override
        public boolean hasRemaining() {
            return remaining() > 0;
        }

        @Override
        public long getBytesAsLong(int count) throws IOException {
            if (count > 8) {
                throw new IllegalArgumentException("Count needs to be between 1 and 8");
            }
            long value = 0;
            int i = 0;
            int byteVal;
            while (i < count && (byteVal = getUnsignedByte()) != -1) {
                value <<= 8;
                value += byteVal;
                i++;
            }
            if (i == count) {
                return value;
            }
            throw new EOFException(String.format("Trying to read %d bytes from slice, but only %d bytes available. Slice=%s", count, i, this));
        }

        //TODO test
        @Override
        public byte[] getBytes(int count) throws IOException {
            byte[] value = new byte[count];
            int i = 0;
            int byteVal;
            while (i < count && (byteVal = getUnsignedByte()) != -1) {
                value[i] = (byte) byteVal;
                i++;
            }
            if (i == count) {
                return value;
            }
            return null;
        }

        @Override
        public String toString() {
            return String.format("%s [pos=%d, lim=%d]", getClass().getSimpleName(), position(), limit());
        }
    }

}
