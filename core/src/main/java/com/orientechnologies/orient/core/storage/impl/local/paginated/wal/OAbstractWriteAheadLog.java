/*
  *
  *  *  Copyright 2014 Orient Technologies LTD (info(at)orientechnologies.com)
  *  *
  *  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  *  you may not use this file except in compliance with the License.
  *  *  You may obtain a copy of the License at
  *  *
  *  *       http://www.apache.org/licenses/LICENSE-2.0
  *  *
  *  *  Unless required by applicable law or agreed to in writing, software
  *  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  *  See the License for the specific language governing permissions and
  *  *  limitations under the License.
  *  *
  *  * For more information: http://www.orientechnologies.com
  *
  */

package com.orientechnologies.orient.core.storage.impl.local.paginated.wal;

import com.orientechnologies.orient.core.exception.OStorageException;

import java.io.IOException;
import java.util.Set;

/**
 * @author Andrey Lomakin <a href="mailto:lomakin.andrey@gmail.com">Andrey Lomakin</a>
 * @since 6/25/14
 */
public abstract class OAbstractWriteAheadLog implements OWriteAheadLog {
  protected boolean            closed;

  protected final Object       syncObject = new Object();
  protected OLogSequenceNumber lastCheckpoint;

  public OLogSequenceNumber logFuzzyCheckPointStart() throws IOException {
    synchronized (syncObject) {
      checkForClose();

      OFuzzyCheckpointStartRecord record = new OFuzzyCheckpointStartRecord(lastCheckpoint);
      log(record);
      return record.getLsn();
    }
  }

  public OLogSequenceNumber logFuzzyCheckPointEnd() throws IOException {
    synchronized (syncObject) {
      checkForClose();

      OFuzzyCheckpointEndRecord record = new OFuzzyCheckpointEndRecord();
      log(record);
      return record.getLsn();
    }
  }

  public OLogSequenceNumber logFullCheckpointStart() throws IOException {
    return log(new OFullCheckpointStartRecord(lastCheckpoint));
  }

  public OLogSequenceNumber logFullCheckpointEnd() throws IOException {
    synchronized (syncObject) {
      checkForClose();

      return log(new OCheckpointEndRecord());
    }
  }

  public void logDirtyPages(Set<ODirtyPage> dirtyPages) throws IOException {
    synchronized (syncObject) {
      checkForClose();

      log(new ODirtyPagesRecord(dirtyPages));
    }
  }

  public OLogSequenceNumber getLastCheckpoint() {
    synchronized (syncObject) {
      checkForClose();

      return lastCheckpoint;
    }
  }

  protected void checkForClose() {
    if (closed)
      throw new OStorageException("WAL has been closed");
  }
}
