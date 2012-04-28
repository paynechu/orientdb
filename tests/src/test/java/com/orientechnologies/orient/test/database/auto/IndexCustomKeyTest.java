/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.ORuntimeKeyIndexDefinition;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.binary.OBinarySerializer;
import com.orientechnologies.orient.core.serialization.serializer.binary.OBinarySerializerFactory;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OBinaryTypeSerializer;

@Test(groups = { "index" })
public class IndexCustomKeyTest {
	private ODatabaseDocumentTx	database;
	private OIndex<?>						index;

	public static class ComparableBinary implements Comparable<ComparableBinary> {
		private byte[]	value;

		public ComparableBinary(byte[] buffer) {
			value = buffer;
		}

		public int compareTo(ComparableBinary o) {
			final int size = value.length;

			for (int i = 0; i < size; ++i) {
				if (value[i] > o.value[i])
					return 1;
				else if (value[i] < o.value[i])
					return -1;
			}
			return 0;
		}

		public byte[] toByteArray() {
			return value;
		}
	}

	public static class OHash256Serializer implements OBinarySerializer<ComparableBinary> {

		public static final OBinaryTypeSerializer	INSTANCE	= new OBinaryTypeSerializer();
		public static final byte									ID				= 100;
		public static final int										LENGTH		= 32;

		public int getObjectSize(final int length) {
			return length;
		}

		public int getObjectSize(final ComparableBinary object) {
			return object.toByteArray().length;
		}

		public void serialize(final ComparableBinary object, final byte[] stream, final int startPosition) {
			final byte[] buffer = object.toByteArray();
			System.arraycopy(buffer, 0, stream, startPosition, buffer.length);
		}

		public ComparableBinary deserialize(final byte[] stream, final int startPosition) {
			final byte[] buffer = Arrays.copyOfRange(stream, startPosition, startPosition + LENGTH);
			return new ComparableBinary(buffer);
		}

		public int getObjectSize(byte[] stream, int startPosition) {
			return LENGTH;
		}

		public byte getId() {
			return ID;
		}
	}

	@BeforeMethod
	public void beforeMethod() {
		database.open("admin", "admin");

		if (index == null) {
			OBinarySerializerFactory.INSTANCE.registerSerializer(new OHash256Serializer(), null);

			index = database.getMetadata().getIndexManager()
					.createIndex("custom-hash", "UNIQUE", new ORuntimeKeyIndexDefinition(OHash256Serializer.ID), null, null);
		} else {
			index = database.getMetadata().getIndexManager().getIndex("custom-hash");
		}
	}

	@AfterMethod
	public void afterMethod() {
		database.close();
	}

	@Parameters(value = "url")
	public IndexCustomKeyTest(String iURL) {
		database = new ODatabaseDocumentTx(iURL);
	}

	public void testUsage() {
		ComparableBinary key1 = new ComparableBinary(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2,
				3, 4, 5, 6, 7, 8, 9, 0, 1 });
		ODocument doc1 = new ODocument().field("k", "key1");
		index.put(key1, doc1);

		ComparableBinary key2 = new ComparableBinary(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2,
				3, 4, 5, 6, 7, 8, 9, 0, 2 });
		ODocument doc2 = new ODocument().field("k", "key1");
		index.put(key2, doc2);

		Assert.assertEquals(index.get(key1), doc1);
		Assert.assertEquals(index.get(key2), doc2);
	}

	public void testUsage2() {
		// EMPTY BUT IT'S ENOUGH TO CALL THE BEFORE_METHOD AND TRY LOADING IT
	}
}