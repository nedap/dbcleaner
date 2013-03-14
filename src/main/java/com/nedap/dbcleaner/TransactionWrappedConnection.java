package com.nedap.dbcleaner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pieter.bos
 */
public class TransactionWrappedConnection extends BaseConnectionWrapper implements ForceableConnection {

    private volatile boolean inForcedTransaction = false;

    private static final Map<Integer, BaseConnectionWrapper> openConnections = new HashMap<Integer, BaseConnectionWrapper>();

    public static List<TransactionWrappedConnection> getOpenConnections() {
        List<TransactionWrappedConnection> connections;
        synchronized (openConnections) {
            connections = new ArrayList(openConnections.values());
        }

        return connections;
    }

    public TransactionWrappedConnection(Connection connection) {
        super(connection);
        synchronized (openConnections) {
            openConnections.put(getConnectionNumber(), this);
        }
        this.inForcedTransaction = TransactionUtil.isInForcedTransaction();
        if (inForcedTransaction) {
            try {
                setAutoCommit(false);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public synchronized void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!this.inForcedTransaction) {
            realConnection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public synchronized void commit() throws SQLException {
        if (!this.inForcedTransaction) {//skip the commit!
            realConnection.commit();
        }
    }

    @Override
    public synchronized void rollback() throws SQLException {
        if (!this.inForcedTransaction) {//skip the commit!
            realConnection.rollback();
        }
    }
    @Override
    public synchronized void close() throws SQLException{
        //don't ever close! But if we should need this:
        /*
         * synchronized (connectionTracker) {
         *      connectionTracker.remove(connectionNumber);
         *  }
         */
    }

    /** force a close on this connection, mainly for cleaning up after tests*/
    public synchronized void forceClose() throws SQLException {
        synchronized (openConnections) {
            openConnections.remove(getConnectionNumber());
        }
        realConnection.close();

    }

    public synchronized void forceStartTransaction() throws SQLException {
        this.inForcedTransaction = true;
        realConnection.setAutoCommit(false);
    }

    public synchronized void forceRollbackTransaction() throws SQLException {
        this.inForcedTransaction = false;
        this.rollback();
    }

    public synchronized void forceCommitTransaction() throws SQLException {
        this.inForcedTransaction = false;
        this.commit();
    }

}
