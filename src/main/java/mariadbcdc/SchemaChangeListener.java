package mariadbcdc;

@FunctionalInterface
public interface SchemaChangeListener {

    void onSchemaChanged(SchemaChangedData schemaChangedData);

}
