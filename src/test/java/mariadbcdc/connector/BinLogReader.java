package mariadbcdc.connector;

import mariadbcdc.BinlogPosition;
import mariadbcdc.connector.io.Either;
import mariadbcdc.connector.packet.ErrPacket;
import mariadbcdc.connector.packet.binlog.BinLogEvent;
import mariadbcdc.connector.packet.binlog.BinlogEventType;
import mariadbcdc.connector.packet.binlog.data.*;

public class BinLogReader {
    private final String host;
    private final int port;
    private final String user;
    private final String password;

    private BinLogSession session;

    private boolean reading = false;
    private BinLogListener listener = BinLogListener.NULL;

    public BinLogReader(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public void setBinlogFilenameAndPosition(String binlogFilename, long position) {

    }

    public void setBinLogListener(BinLogListener listener) {
        if (listener != null) {
            this.listener = listener;
        } else {
            this.listener = BinLogListener.NULL;
        }
    }

    public void connect() {
        session = new BinLogSession(host, port, user, password);
        session.handshake();
        session.fetchBinlogFilenameAndPosition();
    }

    public void start() {
        session.registerSlave();
        read();
    }

    public void read() {
        reading = true;
        try {
            while (reading) {
                Either<ErrPacket, BinLogEvent> readEvent = session.readBinlog();
                try {
                    if (readEvent.isLeft()) {
                        listener.onErr(readEvent.getLeft());
                    } else {
                        BinLogEvent event = readEvent.getRight();
                        if (event.getHeader().getEventType() == BinlogEventType.ROTATE_EVENT) {
                            listener.onRotateEvent(event.getHeader(), (RotateEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.FORMAT_DESCRIPTION_EVENT) {
                            listener.onFormatDescriptionEvent(event.getHeader(), (QueryEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.QUERY_EVENT) {
                            listener.onQueryEvent(event.getHeader(), (QueryEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.TABLE_MAP_EVENT) {
                            listener.onTableMapEvent(event.getHeader(), (TableMapEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.WRITE_ROWS_EVENT_V1) {
                            listener.onWriteRowsEvent(event.getHeader(), (WriteRowsEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.UPDATE_ROWS_EVENT_V1) {
                            listener.onUpdateRowsEvent(event.getHeader(), (UpdateRowsEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.DELETE_ROWS_EVENT_V1) {
                            listener.onDeleteRowsEvent(event.getHeader(), (DeleteRowsEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.XID_EVENT) {
                            listener.onXidEvent(event.getHeader(), (XidEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.HEARTBEAT_LOG_EVENT) {
                            listener.onHeartbeatEvent(event.getHeader(), (HeartbeatEvent) event.getData());
                        } else if (event.getHeader().getEventType() == BinlogEventType.STOP_EVENT) {
                            reading = false;
                            listener.onStopEvent(event.getHeader(), (StopEvent) event.getData());
                        }
                    }
                } catch (Exception ex) {
                    // ignore listener error
                }
            }
        } finally {
            reading = false;
        }
    }

    public BinlogPosition getPosition() {
        return new BinlogPosition(session.getBinlogFile(), session.getBinlogPosition());
    }

    public void disconnect() {
        if (reading) reading = false;
        session.close();
    }

}
