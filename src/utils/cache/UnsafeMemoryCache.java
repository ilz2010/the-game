package utils.cache;

import java.util.concurrent.Semaphore;
import sun.misc.Unsafe;

public class UnsafeMemoryCache {
 private static final Unsafe unsafe = JavaInternals.getUnsafe();
 private static final int BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
 private static final int WRITE_PERMITS = 1024;

 private static final int MAX_KEY_COUNT = 256;
 private static final int KEY_SIZE = 8;
 private static final int KEY_SPACE = MAX_KEY_COUNT * KEY_SIZE;
 private static final int DATA_START = KEY_SPACE * 2;
 private static final int OFFSET = KEY_SPACE + 0;
 private static final int LENGTH = KEY_SPACE + 4;

 private final MappedFile mmap;
 private final int segmentSize;
 private final int segmentMask;
 private Segment[] segments;

 static final class Segment extends Semaphore {
  final long start;
  int tail;
  int count;

  Segment ( long start, int size ) {
   super(WRITE_PERMITS, true);
   this.start = start;
   verify(start, size);
  }

  private void verify ( long start, int size ) {
   int maxTail = DATA_START;
   long prevKey = 0;
   long pos = start;

   for ( long keysEnd = start + KEY_SPACE ; pos < keysEnd ; pos += KEY_SIZE ) {
    long key = unsafe.getLong(pos);
    if ( key <= prevKey ) {
     break;
    }

    int offset = unsafe.getInt(pos + OFFSET);
    int length = unsafe.getInt(pos + LENGTH);
    int newTail = (offset + length + 7) & ~7;
    if ( offset < DATA_START || length < 0 || newTail > size ) {
     break;
    }

    if ( newTail > maxTail ) {
     maxTail = newTail;
    }
    prevKey = key;
   }

   this.tail = maxTail;
   this.count = (int) (pos - start) >>> 3;
  }
 }

 public UnsafeMemoryCache () throws Exception {
  this(new MemoryCacheConfiguration());
 }

 public UnsafeMemoryCache ( MemoryCacheConfiguration configuration ) throws Exception {
  long requestedCapacity = configuration.getCapacity();
  long desiredSegmentSize = configuration.getSegmentSize();
  int segmentCount = calculateSegmentCount(requestedCapacity, desiredSegmentSize);
  long segmentSize1 = (requestedCapacity / segmentCount + 31) & ~31L;

  this.mmap = new MappedFile(configuration.getImageFile(), segmentSize1 * segmentCount);
  this.segmentSize = (int) segmentSize1;
  this.segmentMask = segmentCount - 1;
  this.segments = new Segment[segmentCount];

  for ( int i = 0 ; i < segmentCount ; i++ ) {
   segments[i] = new Segment(mmap.getAddr() + segmentSize1 * i, this.segmentSize);
  }
 }

 public void close () {
  mmap.close();
  segments = null;
 }

 public byte[] get ( long key ) {
  Segment segment = segmentFor(key);
  segment.acquireUninterruptibly();
  try {
   long segmentStart = segment.start;
   long keysEnd = segmentStart + (segment.count << 3);
   long keyAddr = binarySearch(key, segmentStart, keysEnd);

   if ( keyAddr > 0 ) {
    int offset = unsafe.getInt(keyAddr + OFFSET);
    int length = unsafe.getInt(keyAddr + LENGTH);
    byte[] result = new byte[length];
    unsafe.copyMemory(null, segmentStart + offset, result, BYTE_ARRAY_OFFSET, length);
    return result;
   }

   return null;
  } finally {
   segment.release();
  }
 }

 public boolean put ( long key, byte[] value ) {
  int length = value.length;
  if ( length >= segmentSize >> 1 ) {
   return false;
  }

  Segment segment = segmentFor(key);
  segment.acquireUninterruptibly(WRITE_PERMITS);
  try {
   long segmentStart = segment.start;
   int tail = segment.tail;
   int newTail = (tail + length + 7) & ~7;

   if ( newTail > segmentSize ) {
    tail = DATA_START;
    newTail = (tail + length + 7) & ~7;
   }

   purgeOverlappingRegion(segment, tail, newTail);

   int count = segment.count;
   if ( count == MAX_KEY_COUNT ) {
    return false;
   }

   long keysEnd = segmentStart + (count << 3);
   long keyAddr = binarySearch(key, segmentStart, keysEnd);
   if ( keyAddr < 0 ) {
    keyAddr = ~keyAddr;
    unsafe.copyMemory(null, keyAddr, null, keyAddr + KEY_SIZE, keysEnd - keyAddr);
    unsafe.copyMemory(null, keyAddr + KEY_SPACE, null, keyAddr + (KEY_SPACE + KEY_SIZE), keysEnd - keyAddr);
    segment.count = count + 1;
   }

   unsafe.putLong(keyAddr, key);
   unsafe.putInt(keyAddr + OFFSET, tail);
   unsafe.putInt(keyAddr + LENGTH, length);
   unsafe.copyMemory(value, BYTE_ARRAY_OFFSET, null, segmentStart + tail, length);

   segment.tail = newTail;
   return true;
  } finally {
   segment.release(WRITE_PERMITS);
  }
 }

 public int count () {
  int count = 0;
  for ( Segment segment : segments ) {
   count += segment.count;
  }
  return count;
 }

 private int calculateSegmentCount ( long requestedCapacity, long segmentSize ) {
  int segmentCount = 1;
  while ( segmentSize * segmentCount < requestedCapacity ) {
   segmentCount <<= 1;
  }
  return segmentCount;
 }

 private Segment segmentFor ( long key ) {
  return segments[((int) (key ^ (key >>> 16))) & segmentMask];
 }

 private static long binarySearch ( long key, long low, long high ) {
  for ( high -= KEY_SIZE ; low <= high ; ) {
   long mid = ((low + high) >>> 1) & ~7L;
   long midVal = unsafe.getLong(mid);

   if ( midVal < key ) {
    low = mid + KEY_SIZE;
   } else if ( midVal > key ) {
    high = mid - KEY_SIZE;
   } else {
    return mid;
   }
  }
  return ~low;
 }

 private static void purgeOverlappingRegion ( Segment segment, int from, int to ) {
  long pos = segment.start + OFFSET;
  int count = segment.count;
  long end = pos + (count << 3);

  for ( long newPos = pos ; pos < end ; pos += KEY_SIZE ) {
   int offset = unsafe.getInt(pos);
   if ( offset >= from && offset < to ) {
    count--;
   } else {
    if ( newPos != pos ) {
     unsafe.putInt(newPos, offset);
     unsafe.putInt(newPos + 4, unsafe.getInt(pos + 4));
     unsafe.putLong(newPos - KEY_SPACE, unsafe.getLong(pos - KEY_SPACE));
    }
    newPos += KEY_SIZE;
   }
  }

  segment.count = count;
 }
}
