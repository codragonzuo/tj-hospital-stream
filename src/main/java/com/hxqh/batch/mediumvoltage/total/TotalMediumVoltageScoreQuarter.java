package com.hxqh.batch.mediumvoltage.total;

import com.hxqh.utils.RemindDateUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.math.BigDecimal;
import java.sql.Types;

import static com.hxqh.constant.Constant.*;

/**
 * 整体中压开关柜运行状况及评分-季度
 *
 * Created by Ocean lin on 2020/3/16.
 *
 * @author Ocean lin
 */
@SuppressWarnings("Duplicates")
public class TotalMediumVoltageScoreQuarter {


    public static void main(String[] args) throws Exception {

        final int[] type = getType();

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        String quarterQuery = "select ASSETYPE,PRODUCTMODEL,LOCATION,avg(SCORE) as SCORE from RE_TOTAL_SCORE_QUARTER " +
                "where ASSETYPE='中压开关设备' and CREATETIME in " + RemindDateUtils.getLastQuarterString() + " group by ASSETYPE,PRODUCTMODEL,LOCATION";

        JDBCInputFormat.JDBCInputFormatBuilder quarterBuilder =
                JDBCInputFormat.buildJDBCInputFormat().setDrivername(DB2_DRIVER_NAME).setDBUrl(DB2_DB_URL)
                        .setQuery(quarterQuery).setRowTypeInfo(new RowTypeInfo(
                        new TypeInformation<?>[]{BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO,
                                BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.BIG_DEC_TYPE_INFO
                        })).setUsername(DB2_USERNAME)
                        .setPassword(DB2_PASSWORD);

        DataSet<Row> quarterRow = env.createInput(quarterBuilder.finish());

        DataSet<Row> sinkRow = quarterRow.map(new MapFunction<Row, Row>() {
            @Override
            public Row map(Row value) throws Exception {
                Row row = new Row(5);
                row.setField(0, value.getField(0).toString());
                row.setField(1, value.getField(1).toString());
                row.setField(2, value.getField(2).toString());
                BigDecimal sum = (BigDecimal) value.getField(3);
                row.setField(3, sum.doubleValue());
                row.setField(4, RemindDateUtils.getLastQuarter());
                return row;
            }
        });

        String insertQuery = "INSERT INTO RE_TOTAL_SCORE_QUARTER (ASSETYPE,PRODUCTMODEL,LOCATION,SCORE,CREATETIME) VALUES(?,?,?,?,?)";
        JDBCOutputFormat.JDBCOutputFormatBuilder outputBuilder =
                JDBCOutputFormat.buildJDBCOutputFormat().setDrivername(DB2_DRIVER_NAME).setDBUrl(DB2_DB_URL)
                        .setQuery(insertQuery).setSqlTypes(type).setUsername(DB2_USERNAME).setPassword(DB2_PASSWORD);
        sinkRow.output(outputBuilder.finish());

        env.execute("TotalMediumVoltageScoreQuarter");
    }


    private static int[] getType() {
        return new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.DOUBLE, Types.VARCHAR};
    }

}

