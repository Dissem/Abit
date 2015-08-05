package ch.dissem.apps.abit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import ch.dissem.bitmessage.repository.JdbcConfig;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.SQLDroidDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by chris on 14.07.15.
 */
public class SQLiteConfig extends JdbcConfig {

    public SQLiteConfig(Context ctx) {
        super(getDbUrl(ctx), "", "");
    }

    private static String getDbUrl(Context ctx) {
        SQLiteDatabase db = ctx.openOrCreateDatabase(Environment.getExternalStorageDirectory()
                + "/jabit.db", Context.MODE_PRIVATE, null);
        ContextHolder.setContext(ctx);
        return "jdbc:sqlite:" + db.getPath() + "?timeout=5";
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties removeLocale = new Properties();
        removeLocale.put(SQLDroidDriver.ADDITONAL_DATABASE_FLAGS, android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        return DriverManager.getConnection(dbUrl, removeLocale);
    }
}
