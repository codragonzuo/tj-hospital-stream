package com.hxqh.task;

import com.alibaba.fastjson.JSON;
import com.hxqh.domain.YxAts;
import com.hxqh.domain.base.IEDEntity;
import com.hxqh.sink.Db2YxAtsSink;
import com.hxqh.transfer.ProcessWaterEmitter;
import com.hxqh.utils.ConvertUtils;
import com.hxqh.utils.DateUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hxqh.constant.Constant.*;

/**
 * Created by Ocean lin on 2020/2/21.
 * <p>
 * 告警    0-无告警  1-是告警
 * 分合闸  0-分闸    1-是合闸
 * <p>
 * <p>
 * 告警    速断和过流
 * 分合闸  开关位置
 *
 * @author Ocean lin
 */
@SuppressWarnings("Duplicates")
public class ProcessYxAtsTask {

    public static void main(String[] args) {

        args = new String[]{"--input-topic", "yxtest", "--bootstrap.servers", "tj-hospital.com:9092",
                "--zookeeper.connect", "tj-hospital.com:2181", "--group.id", "yxtest"};

        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < NUM_4) {
            System.out.println("Missing parameters!\n" +
                    "Usage: Kafka --input-topic <topic>" +
                    "--bootstrap.servers <kafka brokers> " +
                    "--zookeeper.connect <zk quorum> --group.id <some id>");
            return;
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.getConfig().disableSysoutLogging();
        env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(4, 10000));
        // create a checkpoint every 5 seconds 非常关键，一定要设置启动检查点！！
        env.enableCheckpointing(5000);
        env.getConfig().setGlobalJobParameters(parameterTool);
        // make parameters available in the web interface
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(10, Time.of(30, TimeUnit.SECONDS)));

        FlinkKafkaConsumer010 flinkKafkaConsumer = new FlinkKafkaConsumer010<>(
                parameterTool.getRequired("input-topic"), new SimpleStringSchema(), parameterTool.getProperties());

        FlinkKafkaConsumerBase kafkaConsumerBase = flinkKafkaConsumer.assignTimestampsAndWatermarks(new ProcessWaterEmitter());
        DataStream<String> input = env.addSource(kafkaConsumerBase);
        DataStream<String> filter = input.filter(s -> {
            IEDEntity entity = JSON.parseObject(s, IEDEntity.class);
            return entity.getCKType().equals(ATS) ? true : false;
        }).name("YX-ATS-Filter");

        filter.addSink(new Db2YxAtsSink()).name("YX-ATS-DB2-Sink");

        persistEs(filter);

        try {
            env.execute("ProcessYxAtsTask");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void persistEs(DataStream<String> input) {
        List<HttpHost> httpHosts = new ArrayList<>();
        httpHosts.add(new HttpHost(ES_HOST, ES_PORT, "http"));
        Date now = new Date();

        ElasticsearchSink.Builder<String> esSinkBuilder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<String>() {
                    public IndexRequest createIndexRequest(String element) {
                        IEDEntity entity = JSON.parseObject(element, IEDEntity.class);
                        YxAts yxAts = ConvertUtils.convert2YxAts(entity);
                        String[] split = yxAts.getAssetYpe().split("-");

                        Map<String, Object> map = new HashMap<>(24);
                        map.put("IEDName", yxAts.getIEDName());
                        map.put("ColTime", DateUtils.formatDate(yxAts.getColTime()));
                        map.put("VariableName", yxAts.getVariableName());
                        map.put("Value", yxAts.getValue());
                        map.put("CreateTime", DateUtils.formatDate(now));
                        map.put("assetYpe", yxAts.getAssetYpe());
                        map.put("productModel", yxAts.getProductModel());
                        map.put("assetYpe1", split[0]);
                        map.put("assetYpe2", split[1]);
                        // todo


                        return Requests.indexRequest().index(INDEX_YX_ATS).type(TYPE_YX_ATS).source(map);
                    }

                    @Override
                    public void process(String element, RuntimeContext ctx, RequestIndexer indexer) {
                        indexer.add(createIndexRequest(element));
                    }
                }
        );
        esSinkBuilder.setBulkFlushMaxActions(1);

        // provide a RestClientFactory for custom configuration on the internally created REST client
        esSinkBuilder.setRestClientFactory(
                restClientBuilder -> restClientBuilder.setMaxRetryTimeoutMillis(300)
        );
        input.addSink(esSinkBuilder.build()).name("YC-ATS-ElasticSearch-Sink");
    }
}
