package mariadbcdc.connector;

public class BinLogErrException extends BinLogException {
    public BinLogErrException(String msg) {
        super(msg);
    }
}
