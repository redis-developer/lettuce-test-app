package io.lettuce.test.workloads;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 *  Workload that uses a connection pool to execute GET and SET commands.
 *  see : https://github.com/redis/lettuce/wiki/Connection-Pooling
 */
public class GetSetPooledWorkload extends BaseWorkload {
    private static Logger log = LoggerFactory.getLogger(GetSetPooledWorkload.class);

    private final RedisClient redis;

    public GetSetPooledWorkload(RedisClient redis, CommonWorkloadOptions options) {
        super(options);
        this.redis = redis;
    }

    @Override
    public void run() {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> redis.connect(), new GenericObjectPoolConfig());

        try (StatefulRedisConnection<String, String> conn = pool.borrowObject()) {

            RedisCommands<String, String> cmd = withMetrics(conn.sync());
            Random random = new Random();

            String payload = PayloadUtils.randomString(options().valueSize());

            for (int i = 0; i < options().iterationCount(); i++) {
                String key = keyGenerator().nextKey();
                if (random.nextDouble() < options().getSetRatio()) {
                    cmd.set(key, payload);
                } else {
                    cmd.get(key);
                }

                delay(options().delayAfterIteration());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // terminating
        pool.close();
     }

}
