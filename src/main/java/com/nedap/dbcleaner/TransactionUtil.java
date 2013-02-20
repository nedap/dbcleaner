package com.nedap.dbcleaner;

import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author pieter.bos
 */
public class TransactionUtil {

    private static boolean started = false;
    private static boolean startedAtLeastOnce = false;

    public static synchronized void startTransactions() {
        if (!started) {
            //start the transaction
            List<TransactionWrappedConnection> connectionList = TransactionWrappedConnection.getOpenConnections();
            for (TransactionWrappedConnection c : connectionList) {
                start(c);
            }

            //wrap the switching connection
            List<SwitchingConnectionWrapper> switchingConnections = SwitchingConnectionWrapper.getOpenConnections();
            for (SwitchingConnectionWrapper c : switchingConnections) {
                start(c);
            }
            started = true;
            startedAtLeastOnce = true;
        }
        //else: we're already in a forced transaction, ignore!

    }

    public static synchronized void rollbackTransactions() {
        if (started) {
            List<TransactionWrappedConnection> connectionList = TransactionWrappedConnection.getOpenConnections();
            for (TransactionWrappedConnection c : connectionList) {
                rollback(c);
            }
            //wrap the switching connection
            List<SwitchingConnectionWrapper> switchingConnections = SwitchingConnectionWrapper.getOpenConnections();
            for (SwitchingConnectionWrapper c : switchingConnections) {
                rollback(c);
            }
            started = false;
        }
        //else: we can't rollback outside of a transaction
    }

    public static synchronized void commitTransactions() {
        if (started) {
            List<TransactionWrappedConnection> connectionList = TransactionWrappedConnection.getOpenConnections();
            for (TransactionWrappedConnection c : connectionList) {
                commit(c);
            }
            //wrap the switching connection
            List<SwitchingConnectionWrapper> switchingConnections = SwitchingConnectionWrapper.getOpenConnections();
            for (SwitchingConnectionWrapper c : switchingConnections) {
                commit(c);
            }
            started = false;
        }
        //else: we can't rollback outside of a transaction
    }

    public static synchronized boolean isInForcedTransaction() {
        return startedAtLeastOnce;
    }

    protected static void start(ForceableConnection c) {
        try {
            c.forceStartTransaction();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected static void commit(ForceableConnection c) {
        try {
            c.forceCommitTransaction();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected static void rollback(ForceableConnection c) {
        try {
            c.forceRollbackTransaction();
        } catch (SQLException ex) {
            ex.printStackTrace();//TODO: replace me!
        }
    }
}
