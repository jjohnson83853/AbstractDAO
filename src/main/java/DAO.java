import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DAO {

    private final Logger logger = LoggerFactory.getLogger(DAO.class);

    private DataSource dataSource = null;

    {
        try {
            InitialContext ic = new InitialContext();
            Context envCtx = (Context) ic.lookup("java:comp/env");
            dataSource = (DataSource) envCtx.lookup("whatevermyjndinameis");
        } catch (NamingException e) {
            throw new RuntimeException("Could not lookup datasource for:" + this.getClass().getName(),e);
        }
    }


    protected void close(Connection conn, PreparedStatement stmt, ResultSet rs) throws SQLException {
        if(rs!=null) {
            rs.close();
        }
        if(stmt!=null) {
            stmt.close();
        }
        conn.close();
    }

    protected void rollback(Connection conn) throws SQLException {
        if (conn != null) {
            conn.rollback();
        }
    }

    /**
     * @return
     */
    private Connection getConnection() throws SQLException {
        Connection conn  = dataSource.getConnection();
        if(conn != null) {
            conn.setAutoCommit(false);
        }
        return conn;
    }

    /**
     *
     * Use for update queries.
     * @param queries
     * @return Number of rows updated.
     */
    protected long with(UpdateQuery ... queries) throws SQLException {
        Connection conn = null;
        PreparedStatement statement = null;
        long recordChanges=0L;
        try {
            conn = getConnection();
            for (UpdateQuery query : queries) {
                try {
                    statement = query.createStatement(conn);
                    recordChanges+=statement.executeUpdate();
                } finally {
                    close(null, statement,null);
                }
            }
            conn.commit();
        } catch (Exception e) {
            rollback(conn);
            throw new RuntimeException(e);
        } finally {
            close(conn, null, null);
        }
        return recordChanges;
    }

    protected <T> T with(SelectQuery<T> query) throws SQLException {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            statement = query.createStatement(conn);
            resultSet = statement.executeQuery();
            return query.handle(resultSet);
        } finally {
            close(conn, statement, resultSet);
        }
    }

    protected interface UpdateQuery {
        PreparedStatement createStatement(Connection connection) throws SQLException;
    }

    protected interface SelectQuery<T> {
        PreparedStatement createStatement(Connection connection) throws SQLException;
        T handle(ResultSet results)     throws SQLException;
    }

    /**
     * Used for getting the next number in a sequence from Oracle for insert queries (UpdateQuery).
     * @param sequenceName
     * @return SeqNum
     */
    protected Long getNextSequence(final String sequenceName)  throws SQLException {
        return with(new SelectQuery<Long>() {
            @Override
            public PreparedStatement createStatement(Connection connection) throws SQLException {
                return connection.prepareStatement("SELECT " + sequenceName + ".nextVal FROM DUAL");
            }

            @Override
            public Long handle(ResultSet results) throws SQLException {
                if (results.next()) {
                    return results.getLong(1);
                } else {
                    return null;
                }
            }
        });
    }
}
