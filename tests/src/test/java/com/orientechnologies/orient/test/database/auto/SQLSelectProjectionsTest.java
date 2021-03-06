/*
 * Copyright 2010-2012 Luca Garulli (l.garulli--at--orientechnologies.com)
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

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentHelper;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.enterprise.channel.binary.OResponseProcessingException;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

@Test(groups = "sql-select")
public class SQLSelectProjectionsTest extends DocumentDBBaseTest {

  @Parameters(value = "url")
  public SQLSelectProjectionsTest(@Optional String url) {
    super(url);
  }

  @Test
  public void queryProjectionOk() {
    List<ODocument> result = database
        .command(
            new OSQLSynchQuery<ODocument>(
                " select nick, followings, followers from Profile where nick is defined and followings is defined and followers is defined"))
        .execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      String[] colNames = d.fieldNames();
      Assert.assertEquals(colNames.length, 3);
      Assert.assertEquals(colNames[0], "nick");
      Assert.assertEquals(colNames[1], "followings");
      Assert.assertEquals(colNames[2], "followers");

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionObjectLevel() {
    OObjectDatabaseTx db = new OObjectDatabaseTx(url);
    db.open("admin", "admin");

    List<ODocument> result = db.getUnderlying().query(
        new OSQLSynchQuery<ODocument>(" select nick, followings, followers from Profile "));

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 3);
      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }

    db.close();
  }

  @Test
  public void queryProjectionLinkedAndFunction() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>("select name.toUppercase(), address.city.country.name from Profile")).execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 2);
      if (d.field("name") != null)
        Assert.assertTrue(d.field("name").equals(((String) d.field("name")).toUpperCase()));

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionSameFieldTwice() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>("select name, name.toUppercase() from Profile where name is not null")).execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 2);
      Assert.assertNotNull(d.field("name"));
      Assert.assertNotNull(d.field("name2"));

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionStaticValues() {
    List<ODocument> result = database
        .command(
            new OSQLSynchQuery<ODocument>(
                "select location.city.country.name, address.city.country.name from Profile where location.city.country.name is not null"))
        .execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {

      Assert.assertNotNull(d.field("location"));
      Assert.assertNull(d.field("address"));

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionPrefixAndAppend() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>(
            "select *, name.prefix('Mr. ').append(' ').append(surname).append('!') as test from Profile where name is not null"))
        .execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertEquals(d.field("test").toString(), "Mr. " + d.field("name") + " " + d.field("surname") + "!");

      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionFunctionsAndFieldOperators() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>("select name.append('.').prefix('Mr. ') as name from Profile where name is not null"))
        .execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 1);
      Assert.assertTrue(d.field("name").toString().startsWith("Mr. "));
      Assert.assertTrue(d.field("name").toString().endsWith("."));

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionAliases() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>(
            "select name.append('!') as 1, surname as 2 from Profile where name is not null and surname is not null")).execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 2);
      Assert.assertTrue(d.field("1").toString().endsWith("!"));
      Assert.assertNotNull(d.field("2"));

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionSimpleValues() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select 10, 'ciao' from Profile LIMIT 1")).execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 2);
      Assert.assertEquals(((Integer) d.field("10")).intValue(), 10l);
      Assert.assertEquals(d.field("ciao"), "ciao");

      Assert.assertNull(d.getClassName());
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionJSON() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select @this.toJson() as json from Profile"))
        .execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 1);
      Assert.assertNotNull(d.field("json"));

      new ODocument().fromJSON((String) d.field("json"));
    }
  }

  @Test
  public void queryProjectionContentCollection() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>("SELECT FLATTEN( outE() ) FROM V WHERE outE() TRAVERSE(1,1) (@class = 'E')")).execute();

    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.getSchemaClass().isSubClassOf("E"));
      Assert.assertEquals(ORecordInternal.getRecordType(d), ODocument.RECORD_TYPE);
    }
  }

  @Test
  public void queryProjectionFlattenError() {
    try {
      database.command(new OSQLSynchQuery<ODocument>("SELECT FLATTEN( out_ ), in_ FROM V WHERE out_ TRAVERSE(1,1) (@class = 'E')"))
          .execute();

      Assert.fail();
    } catch (OCommandSQLParsingException e) {

    } catch (OResponseProcessingException e) {
      Assert.assertTrue(e.getCause() instanceof OCommandSQLParsingException);
    }
  }

  public void queryProjectionRid() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select @rid FROM V")).execute();
    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 1);
      Assert.assertNotNull(d.field("rid"));

      final ORID rid = d.field("rid", ORID.class);
      Assert.assertTrue(rid.isValid());
    }
  }

  public void queryProjectionOrigin() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select @raw FROM V")).execute();
    Assert.assertTrue(result.size() != 0);

    for (ODocument d : result) {
      Assert.assertTrue(d.fieldNames().length <= 1);
      Assert.assertNotNull(d.field("raw"));
    }
  }

  public void queryProjectionEval() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select eval('1 + 4') as result")).execute();
    Assert.assertEquals(result.size(), 1);

    for (ODocument d : result)
      Assert.assertEquals(d.field("result"), 5);

  }

  @SuppressWarnings("unchecked")
  public void queryProjectionContextArray() {
    List<ODocument> result = database.command(
        new OSQLSynchQuery<ODocument>("select $a[0] as a0, $a as a from V let $a = outE() where outE().size() > 0")).execute();
    Assert.assertFalse(result.isEmpty());

    for (ODocument d : result) {
      Assert.assertTrue(d.containsField("a"));
      Assert.assertTrue(d.containsField("a0"));

      final ODocument a0doc = d.field("a0");
      final ODocument firstADoc = (ODocument) d.<Iterable<OIdentifiable>> field("a").iterator().next();

      Assert.assertTrue(ODocumentHelper.hasSameContentOf(a0doc, database, firstADoc, database, null));
    }
  }

  public void ifNullFunction() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("SELECT ifnull('a', 'b')")).execute();
    Assert.assertFalse(result.isEmpty());
    Assert.assertEquals(result.get(0).field("ifnull"), "a");

    result = database.command(new OSQLSynchQuery<ODocument>("SELECT ifnull('a', 'b', 'c')")).execute();
    Assert.assertFalse(result.isEmpty());
    Assert.assertEquals(result.get(0).field("ifnull"), "c");

    result = database.command(new OSQLSynchQuery<ODocument>("SELECT ifnull(null, 'b')")).execute();
    Assert.assertFalse(result.isEmpty());
    Assert.assertEquals(result.get(0).field("ifnull"), "b");
  }

  public void filteringArrayInChain() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("SELECT set(name)[0-1] as set from OUser")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(OMultiValue.isMultiValue(d.field("set")));
      Assert.assertTrue(OMultiValue.getSize(d.field("set")) <= 2);
    }

    result = database.command(new OSQLSynchQuery<ODocument>("SELECT set(name)[0,1] as set from OUser")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(OMultiValue.isMultiValue(d.field("set")));
      Assert.assertTrue(OMultiValue.getSize(d.field("set")) <= 2);
    }

    result = database.command(new OSQLSynchQuery<ODocument>("SELECT set(name)[0] as unique from OUser")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertFalse(OMultiValue.isMultiValue(d.field("unique")));
    }
  }

  public void projectionWithNoTarget() {
    List<ODocument> result = database.command(new OSQLSynchQuery<ODocument>("select 'Ay' as a , 'bEE'")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(d.field("a").equals("Ay"));
      Assert.assertTrue(d.field("bEE").equals("bEE"));
    }

    result = database.command(new OSQLSynchQuery<ODocument>("select 'Ay' as a , 'bEE' as b")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(d.field("a").equals("Ay"));
      Assert.assertTrue(d.field("b").equals("bEE"));
    }

    result = database.command(new OSQLSynchQuery<ODocument>("select 'Ay' as a , 'bEE' as b fetchplan *:1")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(d.field("a").equals("Ay"));
      Assert.assertTrue(d.field("b").equals("bEE"));
    }

    result = database.command(new OSQLSynchQuery<ODocument>("select 'Ay' as a , 'bEE' fetchplan *:1")).execute();
    Assert.assertEquals(result.size(), 1);
    for (ODocument d : result) {
      Assert.assertTrue(d.field("a").equals("Ay"));
      Assert.assertTrue(d.field("bEE").equals("bEE"));
    }
  }
}
