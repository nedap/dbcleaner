package com.nedap.dbcleaner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Connection that switches between an actual database connection, and a transaction wrapped connection. Can be used to
 * switch back and forth between transaction wrapping and normal functionality
 *
 *
 * @author pieter.bos
 */
public class SwitchingConnectionWrapper extends BaseConnectionWrapper implements ForceableConnection {

    private volatile boolean inForcedTransaction = false;
    private final Connection actualConnection;
    private final Connection wrappedConnection;
    private static final Map<Integer, SwitchingConnectionWrapper> openConnections = new HashMap<Integer, SwitchingConnectionWrapper>();

    public static List<SwitchingConnectionWrapper> getOpenConnections() {
        List<SwitchingConnectionWrapper> connections;
        synchronized (openConnections) {
            connections = new ArrayList(openConnections.values());
        }
        return connections;
    }

    public SwitchingConnectionWrapper(Connection connection, Connection wrappedConnection, String url, Properties properties) {
        super(connection == null ? wrappedConnection : connection);
        this.actualConnection = connection;
        this.wrappedConnection = wrappedConnection;
        synchronized (openConnections) {
            openConnections.put(getConnectionNumber(), this);
        }

        inForcedTransaction = TransactionUtil.isInForcedTransaction();

        if (inForcedTransaction) {
            //get the underlying transaction and use that!
            realConnection = wrappedConnection;
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        //close the actual connection, not the wrapped one :)
        if (actualConnection != null) {
            actualConnection.close();
        }
        synchronized (openConnections) {
            openConnections.remove(this.getConnectionNumber());
        }
    }

    @Override
    public synchronized void forceStartTransaction() throws SQLException {
        realConnection = wrappedConnection;
        this.inForcedTransaction = true;
        if (actualConnection != null && !actualConnection.isClosed()) {
            if (!actualConnection.getAutoCommit()) {
                actualConnection.rollback();//make sure we rollback the connection
            }
            actualConnection.close();
        }

    }

    @Override
    public synchronized void setAutoCommit(boolean commit) throws SQLException {
        super.setAutoCommit(commit);
    }

    @Override
    public synchronized void forceRollbackTransaction() throws SQLException {
    }

    @Override
    public synchronized void forceCommitTransaction() throws SQLException {
    }
}
