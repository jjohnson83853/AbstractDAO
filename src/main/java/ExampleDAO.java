import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


public class ExampleDAO {

    private final Logger logger = LoggerFactory.getLogger(ExampleDAO.class);
    void main() {
        try {
            new DAO().with(
                    new DAO.UpdateQuery() {
                        @Override
                        public PreparedStatement createStatement(Connection connection) throws SQLException {
                            //could use this as an insert.
                            //This does use an extra connection and therefore won't be part of the same
                            //transaction. Certainly we could do better here.
                            Long myNextSequence = new DAO().getNextSequence("SomeSequence");
                            return connection.prepareStatement("blah");
                        }
                    }
            );
        } catch(SQLException e) {
            logger.error("Could not update", e);
        }


    try {
        List<Map<String,Object>> myCollection = new DAO().with(new DAO.SelectQuery<List<Map<String,Object>>>() {
            @Override
            public PreparedStatement createStatement(Connection connection) throws SQLException {
                return connection.prepareStatement("some select statement");
            }

            @Override
            public List<Map<String,Object>> handle(ResultSet results) throws SQLException {
                //Typically would return a collection or a single value here so client.
                //ResultSet will be closed and the end of this so the ResultSet needs to be
                //handdled here.

                return null;
            }
        });
    } catch(SQLException e) {
        logger.error("Could not update", e);
    }
}
}
