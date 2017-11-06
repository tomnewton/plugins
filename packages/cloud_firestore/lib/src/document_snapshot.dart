// Copyright 2017, the Chromium project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of cloud_firestore;

/// A DocumentSnapshot contains data read from a document in your Firestore
/// database.
///
/// The data can be extracted with the data property or by using subscript
/// syntax to access a specific field.
class DocumentSnapshot {
  /// Represents the document's reference
  final String path;

  /// Contains all the data of this snapshot
  final Map<String, dynamic> data;
  final String path;

<<<<<<< HEAD:packages/firebase_firestore/lib/src/document_snapshot.dart
  DocumentSnapshot._(this.data, this.path);
=======
  DocumentSnapshot._(this.path, this.data);
>>>>>>> 46173b6aa4124d0fa95fedfa44ab9b25271064d4:packages/cloud_firestore/lib/src/document_snapshot.dart

  /// Reads individual values from the snapshot
  dynamic operator [](String key) => data[key];

  /// Returns the ID of the snapshot's document
  String get documentID => path.split('/').last;
}
