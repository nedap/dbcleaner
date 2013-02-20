Tina the database cleaner
=========

Tina cleans your database, so you can run tests without worrying about cleaning up. It does so by wrapping all queries from all connections in a single transaction. It then rolls back or commits at your request. This is particularly useful for testing through the UI or web interface of your application. We use it for running Cucumber stories. It basically does the same as https://github.com/bmabey/database_cleaner, but for JDBC instead of Ruby.

Tina works with JDBC and Java.

It is implemented as a wrapper around your JDBC connection. 
When you start Tina, it wraps completely transparently around your JDBC connections. Then when you want Tina to start, you do:

```java
TransactionUtil.startTransactions();
```

And it will start a transaction. 
Any rollbacks or commits from your application will be ignored.
All database connections from your application will be routed through one underlying connection from now on.

Now when you are finished with your test, simply do:

```java
TransactionUtil.rollbackTransactions();
```

And your database will be fully restored to the state before tests, by issuing a rollback to your database.

Alternatively, you can commit the changes, if you want:
```java
TransactionUtil.commitTransactions();
```
After this call, Tina will no longer be fully transparent. All database connections will still be routed through one underlying connection. This means rollback, transaction isolation and commits will be different from before the first call to `TransactionUtil.startTransactions()`.

##Usage


Use the following as your jdbc-driver:

```
com.nedap.dbcleaner.DBCleaner
```

Then use the following URL:

```
jdbc:dbcleaner:<DBTYPE>://<HOST>/<DB>
```

For example:

```
jdbc:dbcleaner:mysql://localhost/database
```

The most common JDBC-drivers are supported out of the box.

```
oracle.jdbc.driver.OracleDriver
oracle.jdbc.OracleDriver
com.sybase.jdbc2.jdbc.SybDriver
net.sourceforge.jtds.jdbc.Driver
com.microsoft.jdbc.sqlserver.SQLServerDriver
weblogic.jdbc.sqlserver.SQLServerDriver
com.informix.jdbc.IfxDriver
org.apache.derby.jdbc.ClientDriver
org.apache.derby.jdbc.EmbeddedDriver
com.mysql.jdbc.Driver
org.postgresql.Driver
org.hsqldb.jdbcDriver
org.h2.Driver
```

Any properties will be forwarded to the actual driver.


##Things that do not (yet) work


Any statement that does an implicit transaction will cause the rollback to fail. For example, in MySQL, altering a table and creating or dropping a table without the temporary keyword creates an implicit transaction rollback.
This means your database is not rolled back. Luckily, in real world-applications, implicit transaction commits are very rare.

Because Tina switches from many underlying JDBC-connections to one when it is first started, you should either not use preparedstatements before the first call to startTransactions(), or recreate them after the first call.
It is very possible to modify Tina to automatically create new PreparedStatements for the new connection, but it is not yet implemented.

##FAQ

###Why doesn't Tina go back to multiple underlying JDBC connections?

We tried going back to multiple underlying JDBC connections, so you keep rollback support after starting then stopping Tina. However, it proved difficult with JDBC to do so, so we opted not to.


###Why doesn't Tina keep multiple connections open with READ_UNCOMMITTED transaction isolation?

This would mean keeping all transactions open until the end of the transaction. We tried this approach, but it introduced many locking issues that were very hard to resolve. The one connection approach is simpler and does not have any locking issues.

An alternative approach could be to dump the contents of every table when it is first modified during your testrun, then restoring this dump after the testrun. In this way you don't have to touch transactions. This would be interesting to test, but it might be a bit slower and much more complicated to implement well.

###Why is this called Tina?

Because Tina is a cleaning lady. Who cleans your database. And it more or less rhymes with cleaner ;).
