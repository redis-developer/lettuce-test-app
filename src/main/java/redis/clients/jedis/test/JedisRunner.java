package redis.clients.jedis.test;

public interface JedisRunner extends AutoCloseable {

    void run();

}
