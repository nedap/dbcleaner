/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nedap.dbcleaner;

import java.sql.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pieter.bos
 */
public class DBCleanerTest {

    Connection connection = null;
    Connection connection2 = null;

    private static int dbConnection = 0;

    @Before
    public void setup() throws Exception {
        Class.forName("com.nedap.dbcleaner.DBCleaner");
        //problem: cannot clean HSQLDB for some reason, connection remains open no matter what we do
        //so just create numbered dbs in memory for each test
        connection = DriverManager.getConnection("jdbc:dbcleaner:hsqldb:mem:dbcleaner.db" + ++dbConnection);
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE test (id int, name varchar(255));");
    }

    @After
    public void teardown() throws Exception {
        //TransactionUtil.rollbackTransactions(); //just in case
        Statement statement = connection.createStatement();
        //delete the DB
        statement.execute("DROP SCHEMA PUBLIC CASCADE;");
        statement.execute("SHUTDOWN;");
        connection.close();
        if(connection2 != null){
            connection2.close();
        }
        //cleanup
        for(TransactionWrappedConnection connection: TransactionWrappedConnection.getOpenConnections()){
            connection.forceClose();
        }

    }

    @Test
    public void testRollback() throws Exception {
        insert(1, "test");
        assertEquals("test", getName(1));
        TransactionUtil.startTransactions();
        update(1, "name2");
        assertEquals("name2", getName(1));
        TransactionUtil.rollbackTransactions();
        assertEquals("test", getName(1));
    }

    @Test
    public void testCommit() throws Exception {
        insert(1, "test");
        assertEquals("test", getName(1));
        TransactionUtil.startTransactions();
        update(1, "name2");
        assertEquals("name2", getName(1));
        TransactionUtil.commitTransactions();
        assertEquals("name2", getName(1));
    }

    /**
     * Test that setAutoCommit on the connection no longer works
     * @throws Exception
     */
    @Test
    public void testSetAutoCommitDisabled() throws Exception {
        TransactionUtil.startTransactions();
        connection.setAutoCommit(true);
        assertFalse(connection.getAutoCommit());
        TransactionUtil.rollbackTransactions();
    }


    @Test
    public void testManualCommitDisabled() throws Exception {
        insert(1, "test");
        TransactionUtil.startTransactions();
        connection.setAutoCommit(false);
        update(1, "changed");
        connection.commit();
        assertEquals("changed", getName(1));
        TransactionUtil.rollbackTransactions();
        assertEquals("test", getName(1));
    }

    /**
     * The following tests that manual rollbacks are disabled in transaction mode
     *
     * This is rather strange, but a side effect of the mechanism that is unavoidable.
     *
     * @throws Exception
     */
    @Test
    public void testManualRollbackDisabled() throws Exception {
        insert(1, "test");
        TransactionUtil.startTransactions();
        connection.setAutoCommit(false);
        update(1, "changed");
        connection.rollback();
        assertEquals("changed", getName(1));
        TransactionUtil.commitTransactions();
        assertEquals("changed", getName(1));
    }

    @Test
    public void testMultipleConnections() throws Exception {
        connection2 = DriverManager.getConnection("jdbc:dbcleaner:hsqldb:mem:dbcleaner.db" + dbConnection);

        insert(1, "test");
        TransactionUtil.startTransactions();
        assertEquals(1, TransactionWrappedConnection.getOpenConnections().size());
        assertEquals(2, SwitchingConnectionWrapper.getOpenConnections().size());
        connection2.createStatement().execute("UPDATE test SET name = 'changed' WHERE id=1");
        insert(2, "second");
        TransactionUtil.rollbackTransactions();
        assertEquals("test", getName(1));
    }

    private void insert(int id, String name) throws Exception {
        Statement statement = connection.createStatement();

        statement.execute("INSERT INTO test (id, name) VALUES (" + id + ", '" + name +"');");
    }

    private void update(int id, String name) throws Exception {
        Statement statement = connection.createStatement();
        statement.executeUpdate("UPDATE test SET name='" + name + "' WHERE id=" + id + ";");
    }

    private String getName(int id) throws Exception {
        ResultSet result = connection.createStatement().executeQuery("SELECT name FROM test WHERE id = " + id + ";");
        result.next();
        return result.getString("name");

    }



}
