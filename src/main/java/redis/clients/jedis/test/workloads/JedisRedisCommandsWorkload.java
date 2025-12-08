package redis.clients.jedis.test.workloads;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;

import java.util.ArrayList;
import java.util.List;

public class JedisRedisCommandsWorkload extends JedisBaseWorkload {

    protected static final Logger log = LoggerFactory.getLogger(JedisRedisCommandsWorkload.class);

    public JedisRedisCommandsWorkload(UnifiedJedis jedis, CommonWorkloadOptions options) {
        super(jedis, options);
    }

    @Override
    protected String getType() {
        return "redis_commands";
    }

    @Override
    protected void doRun() {
        String payload = PayloadUtils.randomString(options().valueSize());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();

            exec("set", c -> c.set(key, payload));
            exec("get", c -> c.get(key));
            exec("del", c -> c.del(key));
            exec("incr", c -> c.incr("counter"));

            List<String> payloads = new ArrayList<>();
            for (int j = 0; j < options().elementsCount(); j++) {
                payloads.add(payload);
            }
            if (options().elementsCount() > 0) {
                exec("lpush", c -> c.lpush(key + "list", payloads.toArray(new String[0])));
                exec("lrange", c -> c.lrange(key + "list", 0, -1));
                exec("ltrim", c -> c.ltrim(key + "list", 0, options().elementsCount()));
            }

            delay(options().delayAfterIteration());
        }
    }

}
