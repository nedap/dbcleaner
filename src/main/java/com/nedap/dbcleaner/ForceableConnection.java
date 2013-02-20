package com.nedap.dbcleaner;

import java.sql.SQLException;

/**
 *
 * @author pieter.bos
 */
public interface ForceableConnection {

    void forceStartTransaction() throws SQLException;

    void forceRollbackTransaction() throws SQLException;

    void forceCommitTransaction() throws SQLException;
}
