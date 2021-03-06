package com.hxqh.sync;


import com.hxqh.utils.JdbcUtil4Db2;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import static com.hxqh.constant.Constant.*;

/**
 * 变压器遥测数据同步
 * <p>
 * Created by Ocean lin on 2020/3/9.
 *
 * @author Ocean lin
 */
@SuppressWarnings("Duplicates")
public class SyncTransformerMySQL2Db2 {

    public static void main(String[] args) throws Exception {
        final TypeInformation<?>[] fieldTypes = getFieldTypes();
        final int[] type = getType();

        final RowTypeInfo rowTypeInfo = new RowTypeInfo(fieldTypes);

        ExecutionEnvironment environment = ExecutionEnvironment.getExecutionEnvironment();

        String selectQuery = "select IEDNAME,COLTIME,APhaseTemperature,BPhaseTemperature,CPhaseTemperature,DRoadTemperature,CREATETIME from yc_transformer_current";

        JDBCInputFormat.JDBCInputFormatBuilder inputBuilder =
                JDBCInputFormat.buildJDBCInputFormat().setDrivername(MYSQL_DRIVER_NAME).setDBUrl(MYSQL_DB_URL)
                        .setQuery(selectQuery).setRowTypeInfo(rowTypeInfo).setUsername(MYSQL_USERNAME)
                        .setPassword(MYSQL_PASSWORD);

        DataSet<Row> source = environment.createInput(inputBuilder.finish());

        Connection connection = JdbcUtil4Db2.getConnection();
        String truncateSql = "TRUNCATE TABLE YC_TRANSFORMER_CURRENT IMMEDIATE";
        PreparedStatement preparedStatement = connection.prepareStatement(truncateSql);
        preparedStatement.execute();
        JdbcUtil4Db2.close(preparedStatement, connection);

        String insertQuery = "INSERT INTO YC_TRANSFORMER_CURRENT (IEDNAME,COLTIME,APHASETEMPERATURE,BPHASETEMPERATURE,CPHASETEMPERATURE,DROADTEMPERATURE,CREATETIME) VALUES(?,?,?,?,?,?,?)";
        JDBCOutputFormat.JDBCOutputFormatBuilder outputBuilder =
                JDBCOutputFormat.buildJDBCOutputFormat().setDrivername(DB2_DRIVER_NAME).setDBUrl(DB2_DB_URL)
                        .setQuery(insertQuery).setSqlTypes(type).setUsername(DB2_USERNAME).setPassword(DB2_PASSWORD);
        source.output(outputBuilder.finish());

        environment.execute("SyncTransformerMySQL2Db2");

    }


    private static TypeInformation<?>[] getFieldTypes() {
        return new TypeInformation<?>[]{BasicTypeInfo.STRING_TYPE_INFO, SqlTimeTypeInfo.TIMESTAMP,
                BasicTypeInfo.DOUBLE_TYPE_INFO, BasicTypeInfo.DOUBLE_TYPE_INFO, BasicTypeInfo.DOUBLE_TYPE_INFO, BasicTypeInfo.DOUBLE_TYPE_INFO,
                SqlTimeTypeInfo.TIMESTAMP
        };
    }


    private static int[] getType() {
        return new int[]{Types.VARCHAR, Types.TIMESTAMP,
                Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.TIMESTAMP
        };
    }
}
