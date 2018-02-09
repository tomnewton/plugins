// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.firebase.cloudfirestore;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import io.flutter.plugin.common.*;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * MessageCodec using an extended version of the Flutter standard binary encoding.
 *
 * <p>This codec is guaranteed to be compatible with the corresponding <a
 * href="https://www.dartdocs.org/documentation/cloud_firestore/latest/cloud_firestore/FirestoreMessageCodec-class.html">FirestoreMessageCodec</a>
 * on the Dart side. These parts of the Flutter SDK are evolved synchronously.
 *
 * <p>Supported messages are acyclic values of these forms:
 *
 * <ul>
 *   <li>null
 *   <li>Booleans
 *   <li>Bytes, Shorts, Integers, Longs, BigIntegers
 *   <li>Floats, Doubles
 *   <li>Strings
 *   <li>byte[], int[], long[], double[]
 *   <li>Lists of supported values
 *   <li>Maps with supported keys and values
 *   <li>Date
 *   <li>GeoPoint
 *   <li>DocumentReference
 * </ul>
 *
 * <p>On the Dart side, these values are represented as follows:
 *
 * <ul>
 *   <li>null: null
 *   <li>Boolean: bool
 *   <li>Byte, Short, Integer, Long, BigInteger: int
 *   <li>Float, Double: double
 *   <li>String: String
 *   <li>byte[]: Uint8List
 *   <li>int[]: Int32List
 *   <li>long[]: Int64List
 *   <li>double[]: Float64List
 *   <li>List: List
 *   <li>Map: Map
 *   <li>DateTime
 *   <li>GeoPoint
 *   <li>DocumentReference
 * </ul>
 */
public final class FirestoreMessageCodec implements MessageCodec<Object> {
  public static final FirestoreMessageCodec INSTANCE = new FirestoreMessageCodec();

  private FirestoreMessageCodec() {}

  @Override
  public ByteBuffer encodeMessage(Object message) {
    if (message == null) {
      return null;
    }
    final ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream();
    writeValue(stream, message);
    final ByteBuffer buffer = ByteBuffer.allocateDirect(stream.size());
    buffer.put(stream.buffer(), 0, stream.size());
    return buffer;
  }

  @Override
  public Object decodeMessage(ByteBuffer message) {
    if (message == null) {
      return null;
    }
    message.order(ByteOrder.nativeOrder());
    final Object value = readValue(message);
    if (message.hasRemaining()) {
      throw new IllegalArgumentException("Message corrupted");
    }
    return value;
  }

  private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
  private static final Charset UTF8 = Charset.forName("UTF8");
  private static final byte NULL = 0;
  private static final byte TRUE = 1;
  private static final byte FALSE = 2;
  private static final byte INT = 3;
  private static final byte LONG = 4;
  private static final byte BIGINT = 5;
  private static final byte DOUBLE = 6;
  private static final byte STRING = 7;
  private static final byte BYTE_ARRAY = 8;
  private static final byte INT_ARRAY = 9;
  private static final byte LONG_ARRAY = 10;
  private static final byte DOUBLE_ARRAY = 11;
  private static final byte LIST = 12;
  private static final byte MAP = 13;
  private static final byte DATE_TIME = 14;
  private static final byte GEO_POINT = 15;
  private static final byte DOCUMENT_REFERENCE = 16;

  private static void writeSize(ByteArrayOutputStream stream, int value) {
    assert 0 <= value;
    if (value < 254) {
      stream.write(value);
    } else if (value <= 0xffff) {
      stream.write(254);
      writeChar(stream, value);
    } else {
      stream.write(255);
      writeInt(stream, value);
    }
  }

  private static void writeChar(ByteArrayOutputStream stream, int value) {
    if (LITTLE_ENDIAN) {
      stream.write(value);
      stream.write(value >>> 8);
    } else {
      stream.write(value >>> 8);
      stream.write(value);
    }
  }

  private static void writeInt(ByteArrayOutputStream stream, int value) {
    if (LITTLE_ENDIAN) {
      stream.write(value);
      stream.write(value >>> 8);
      stream.write(value >>> 16);
      stream.write(value >>> 24);
    } else {
      stream.write(value >>> 24);
      stream.write(value >>> 16);
      stream.write(value >>> 8);
      stream.write(value);
    }
  }

  private static void writeLong(ByteArrayOutputStream stream, long value) {
    if (LITTLE_ENDIAN) {
      stream.write((byte) value);
      stream.write((byte) (value >>> 8));
      stream.write((byte) (value >>> 16));
      stream.write((byte) (value >>> 24));
      stream.write((byte) (value >>> 32));
      stream.write((byte) (value >>> 40));
      stream.write((byte) (value >>> 48));
      stream.write((byte) (value >>> 56));
    } else {
      stream.write((byte) (value >>> 56));
      stream.write((byte) (value >>> 48));
      stream.write((byte) (value >>> 40));
      stream.write((byte) (value >>> 32));
      stream.write((byte) (value >>> 24));
      stream.write((byte) (value >>> 16));
      stream.write((byte) (value >>> 8));
      stream.write((byte) value);
    }
  }

  private static void writeDouble(ByteArrayOutputStream stream, double value) {
    writeLong(stream, Double.doubleToLongBits(value));
  }

  private static void writeBytes(ByteArrayOutputStream stream, byte[] bytes) {
    writeSize(stream, bytes.length);
    stream.write(bytes, 0, bytes.length);
  }

  private static void writeAlignment(ByteArrayOutputStream stream, int alignment) {
    final int mod = stream.size() % alignment;
    if (mod != 0) {
      for (int i = 0; i < alignment - mod; i++) {
        stream.write(0);
      }
    }
  }

  static void writeValue(ByteArrayOutputStream stream, Object value) {
    if (value == null) {
      stream.write(NULL);
    } else if (value == Boolean.TRUE) {
      stream.write(TRUE);
    } else if (value == Boolean.FALSE) {
      stream.write(FALSE);
    } else if (value instanceof Number) {
      if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
        stream.write(INT);
        writeInt(stream, ((Number) value).intValue());
      } else if (value instanceof Long) {
        stream.write(LONG);
        writeLong(stream, (long) value);
      } else if (value instanceof Float || value instanceof Double) {
        stream.write(DOUBLE);
        writeAlignment(stream, 8);
        writeDouble(stream, ((Number) value).doubleValue());
      } else if (value instanceof BigInteger) {
        stream.write(BIGINT);
        writeBytes(stream, ((BigInteger) value).toString(16).getBytes(UTF8));
      } else {
        throw new IllegalArgumentException("Unsupported Number type: " + value.getClass());
      }
    } else if (value instanceof String) {
      stream.write(STRING);
      writeBytes(stream, ((String) value).getBytes(UTF8));
    } else if (value instanceof byte[]) {
      stream.write(BYTE_ARRAY);
      writeBytes(stream, (byte[]) value);
    } else if (value instanceof Blob){
      stream.write(BYTE_ARRAY);
      writeBytes(stream, ((Blob)value).toBytes());
    } else if (value instanceof int[]) {
      stream.write(INT_ARRAY);
      final int[] array = (int[]) value;
      writeSize(stream, array.length);
      writeAlignment(stream, 4);
      for (final int n : array) {
        writeInt(stream, n);
      }
    } else if (value instanceof long[]) {
      stream.write(LONG_ARRAY);
      final long[] array = (long[]) value;
      writeSize(stream, array.length);
      writeAlignment(stream, 8);
      for (final long n : array) {
        writeLong(stream, n);
      }
    } else if (value instanceof double[]) {
      stream.write(DOUBLE_ARRAY);
      final double[] array = (double[]) value;
      writeSize(stream, array.length);
      writeAlignment(stream, 8);
      for (final double d : array) {
        writeDouble(stream, d);
      }
    } else if (value instanceof List) {
      stream.write(LIST);
      final List<?> list = (List) value;
      writeSize(stream, list.size());
      for (final Object o : list) {
        writeValue(stream, o);
      }
    } else if (value instanceof Map) {
      stream.write(MAP);
      final Map<?, ?> map = (Map) value;
      writeSize(stream, map.size());
      for (final Entry entry : map.entrySet()) {
        writeValue(stream, entry.getKey());
        writeValue(stream, entry.getValue());
      }
    } else if (value instanceof Date) {
      stream.write(DATE_TIME);
      final Date date = (Date) value;
      final long milliseconds = date.getTime();
      writeLong(stream, milliseconds * 1000);
    } else if (value instanceof GeoPoint) {
      stream.write(GEO_POINT);
      writeAlignment(stream, 8);
      final GeoPoint g = (GeoPoint) value;
      writeDouble(stream, g.getLatitude());
      writeDouble(stream, g.getLongitude());
    } else if (value instanceof DocumentReference) {
      stream.write(DOCUMENT_REFERENCE);
      final DocumentReference d = (DocumentReference) value;
      writeBytes(stream, (d.getPath()).getBytes(UTF8));
    } else {
      throw new IllegalArgumentException("Unsupported value: " + value);
    }
  }

  private static int readSize(ByteBuffer buffer) {
    if (!buffer.hasRemaining()) {
      throw new IllegalArgumentException("Message corrupted");
    }
    final int value = buffer.get() & 0xff;
    if (value < 254) {
      return value;
    } else if (value == 254) {
      return buffer.getChar();
    } else {
      return buffer.getInt();
    }
  }

  private static byte[] readBytes(ByteBuffer buffer) {
    final int length = readSize(buffer);
    final byte[] bytes = new byte[length];
    buffer.get(bytes);
    return bytes;
  }

  private static void readAlignment(ByteBuffer buffer, int alignment) {
    final int mod = buffer.position() % alignment;
    if (mod != 0) {
      buffer.position(buffer.position() + alignment - mod);
    }
  }

  static Object readValue(ByteBuffer buffer) {
    if (!buffer.hasRemaining()) {
      throw new IllegalArgumentException("Message corrupted");
    }
    final Object result;
    switch (buffer.get()) {
      case NULL:
        result = null;
        break;
      case TRUE:
        result = true;
        break;
      case FALSE:
        result = false;
        break;
      case INT:
        result = buffer.getInt();
        break;
      case LONG:
        result = buffer.getLong();
        break;
      case BIGINT:
        {
          final byte[] hex = readBytes(buffer);
          result = new BigInteger(new String(hex, UTF8), 16);
          break;
        }
      case DOUBLE:
        readAlignment(buffer, 8);
        result = buffer.getDouble();
        break;
      case STRING:
        {
          final byte[] bytes = readBytes(buffer);
          result = new String(bytes, UTF8);
          break;
        }
      case BYTE_ARRAY:
        {
          result = readBytes(buffer);
          break;
        }
      case INT_ARRAY:
        {
          final int length = readSize(buffer);
          final int[] array = new int[length];
          readAlignment(buffer, 4);
          buffer.asIntBuffer().get(array);
          result = array;
          buffer.position(buffer.position() + 4 * length);
          break;
        }
      case LONG_ARRAY:
        {
          final int length = readSize(buffer);
          final long[] array = new long[length];
          readAlignment(buffer, 8);
          buffer.asLongBuffer().get(array);
          result = array;
          buffer.position(buffer.position() + 8 * length);
          break;
        }
      case DOUBLE_ARRAY:
        {
          final int length = readSize(buffer);
          final double[] array = new double[length];
          readAlignment(buffer, 8);
          buffer.asDoubleBuffer().get(array);
          result = array;
          buffer.position(buffer.position() + 8 * length);
          break;
        }
      case LIST:
        {
          final int size = readSize(buffer);
          final List<Object> list = new ArrayList<>(size);
          for (int i = 0; i < size; i++) {
            list.add(readValue(buffer));
          }
          result = list;
          break;
        }
      case MAP:
        {
          final int size = readSize(buffer);
          final Map<Object, Object> map = new HashMap<>();
          for (int i = 0; i < size; i++) {
            map.put(readValue(buffer), readValue(buffer));
          }
          result = map;
          break;
        }
      case DATE_TIME:
        {
          final long microseconds = buffer.getLong();
          result = new Date(microseconds / 1000);
          break;
        }
      case GEO_POINT:
        {
          readAlignment(buffer, 8);
          final double latitude = buffer.getDouble();
          final double longitude = buffer.getDouble();
          result = new GeoPoint(latitude, longitude);
          break;
        }
      case DOCUMENT_REFERENCE:
        {
          final byte[] bytes = readBytes(buffer);
          final String path = new String(bytes, UTF8);
          result = FirebaseFirestore.getInstance().document(path);
          break;
        }
      default:
        throw new IllegalArgumentException("Message corrupted");
    }
    return result;
  }

  static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
    byte[] buffer() {
      return buf;
    }
  }
}