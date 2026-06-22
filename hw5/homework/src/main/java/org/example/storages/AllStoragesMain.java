package org.example.storages;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.sql.DataSource;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ДЗ: использовать ВСЕ типы хранилищ + доп.задание — подключить Hazelcast.
 *
 * Одно приложение по очереди обращается к каждому хранилищу (запись + чтение):
 *   1) PostgreSQL  — OLTP, реляционная        (JdbcTemplate)
 *   2) ClickHouse  — OLAP, колоночная          (JdbcTemplate, batch insert + агрегаты)
 *   3) Redis       — key-value                 (StringRedisTemplate)
 *   4) MongoDB     — документная               (MongoTemplate)
 *   5) S3 / MinIO  — объектное                 (AWS SDK v2 S3Client)
 *   6) Hazelcast   — in-memory data grid       (embedded, IMap) [доп.задание]
 *
 * Конфигурация всех хранилищ — вручную (@Bean), контекст — AnnotationConfigApplicationContext,
 * чтобы несколько Spring Data модулей в одном приложении не конфликтовали через автоконфигурацию.
 *
 * Параметры подключения берутся из переменных окружения (значения по умолчанию — localhost).
 */
@Configuration
public class AllStoragesMain {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(AllStoragesMain.class)) {

            run("PostgreSQL (OLTP, реляционная)",
                () -> postgresDemo(ctx.getBean("postgresJdbcTemplate", JdbcTemplate.class)));
            run("ClickHouse (OLAP, колоночная)",
                () -> clickhouseDemo(ctx.getBean("clickhouseJdbcTemplate", JdbcTemplate.class)));
            run("Redis (key-value)",
                () -> redisDemo(ctx.getBean(StringRedisTemplate.class)));
            run("MongoDB (документная)",
                () -> mongoDemo(ctx.getBean(MongoTemplate.class)));
            run("S3 / MinIO (объектное)",
                () -> s3Demo(ctx.getBean(S3Client.class)));
            run("Hazelcast (in-memory data grid) [доп.задание]",
                () -> hazelcastDemo(ctx.getBean(HazelcastInstance.class)));
        }
    }

    // ===================== демо по каждому хранилищу =====================

    static void postgresDemo(JdbcTemplate jdbc) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id     BIGSERIAL PRIMARY KEY,
                name   VARCHAR(50) NOT NULL,
                about  VARCHAR(100)
            )""");
        jdbc.update("TRUNCATE users RESTART IDENTITY");
        jdbc.update("INSERT INTO users (name, about) VALUES (?, ?)", "John Doe", "Engineer");
        jdbc.update("INSERT INTO users (name, about) VALUES (?, ?)", "Jane Doe", "Designer");

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        System.out.println("rows in users: " + count);
        jdbc.query("SELECT id, name, about FROM users ORDER BY id",
                (rs, n) -> "  #" + rs.getLong("id") + " " + rs.getString("name") + " — " + rs.getString("about"))
            .forEach(System.out::println);
    }

    static void clickhouseDemo(JdbcTemplate jdbc) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS events (
                id    Int64,
                name  String,
                ts    DateTime DEFAULT now()
            ) ENGINE = MergeTree ORDER BY id""");
        jdbc.update("TRUNCATE TABLE events");

        // ClickHouse любит пакетные вставки — заливаем 1000 строк одним batch-ем
        List<Object[]> batch = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            batch.add(new Object[]{i, (i % 2 == 0) ? "even" : "odd"});
        }
        jdbc.batchUpdate("INSERT INTO events (id, name) VALUES (?, ?)", batch);

        Long total = jdbc.queryForObject("SELECT count() FROM events", Long.class);
        System.out.println("rows in events: " + total);
        System.out.println("aggregate (GROUP BY name):");
        jdbc.query("SELECT name, count() AS c FROM events GROUP BY name ORDER BY name",
                (rs, n) -> "  " + rs.getString("name") + " = " + rs.getLong("c"))
            .forEach(System.out::println);
    }

    static void redisDemo(StringRedisTemplate redis) {
        redis.opsForValue().set("user:1", "John Doe", Duration.ofMinutes(10));
        String value = redis.opsForValue().get("user:1");
        Long ttl = redis.getExpire("user:1");
        System.out.println("GET user:1 = " + value + "  (ttl ≈ " + ttl + "s)");
    }

    static void mongoDemo(MongoTemplate mongo) {
        String id = UUID.randomUUID().toString();
        Author saved = mongo.save(new Author(id, "John Doe"));
        Author found = mongo.findById(id, Author.class);
        System.out.println("saved:  " + saved);
        System.out.println("found:  " + found);
        System.out.println("count in 'authors': " + mongo.count(new org.springframework.data.mongodb.core.query.Query(), Author.class));
    }

    static void s3Demo(S3Client s3) {
        String bucket = "demo-bucket";
        String key = "hello.txt";
        String content = "Hello from MinIO/S3! ts=" + System.currentTimeMillis();

        if (s3.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(bucket))) {
            s3.createBucket(b -> b.bucket(bucket));
        }
        s3.putObject(r -> r.bucket(bucket).key(key), RequestBody.fromString(content));

        ResponseBytes<GetObjectResponse> obj = s3.getObjectAsBytes(r -> r.bucket(bucket).key(key));
        System.out.println("PUT  s3://" + bucket + "/" + key);
        System.out.println("GET  -> " + obj.asUtf8String());
    }

    static void hazelcastDemo(HazelcastInstance hz) {
        IMap<String, String> map = hz.getMap("demo");
        map.put("greeting", "hello");
        map.put("project", "Seminar5");
        System.out.println("cluster: " + hz.getCluster().getMembers().size() + " member(s)");
        System.out.println("IMap 'demo' get(greeting) = " + map.get("greeting"));
        System.out.println("IMap 'demo' size = " + map.size());
    }

    // ===================== бины хранилищ =====================

    @Bean
    public DataSource postgresDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(env("PG_URL", "jdbc:postgresql://localhost:5432/mydatabase"));
        ds.setUsername(env("PG_USER", "admin"));
        ds.setPassword(env("PG_PASSWORD", "password"));
        return ds;
    }

    @Bean
    public JdbcTemplate postgresJdbcTemplate() {
        return new JdbcTemplate(postgresDataSource());
    }

    @Bean
    public DataSource clickhouseDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setUrl(env("CH_URL", "jdbc:clickhouse://localhost:8123/mydatabase?compress=false"));
        ds.setUsername(env("CH_USER", "admin"));
        ds.setPassword(env("CH_PASSWORD", "password"));
        return ds;
    }

    @Bean
    public JdbcTemplate clickhouseJdbcTemplate() {
        return new JdbcTemplate(clickhouseDataSource());
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(env("REDIS_HOST", "localhost"), envInt("REDIS_PORT", 6379));
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        return MongoClients.create(
            env("MONGO_URI", "mongodb://admin:password@localhost:27017/mydatabase?authSource=admin"));
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient client) {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, "mydatabase"));
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(
            env("S3_ACCESS_KEY", "minioAccessKey"), env("S3_SECRET_KEY", "minioSecretKey"));
        return S3Client.builder()
            .endpointOverride(URI.create(env("S3_ENDPOINT", "http://localhost:9090")))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .forcePathStyle(true)
            .build();
    }

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName("dev");
        // встроенный (embedded) одиночный член — отключаем поиск соседей по сети
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.getJetConfig().setEnabled(false);
        return Hazelcast.newHazelcastInstance(config);
    }

    // ===================== вспомогательное =====================

    interface Demo {
        void run() throws Exception;
    }

    static void run(String title, Demo demo) {
        System.out.println("\n===== " + title + " =====");
        // Несколько попыток: при старте «всё в Docker» хранилища (ClickHouse, Mongo)
        // прогреваются на пару секунд дольше приложения — ждём и повторяем.
        int maxAttempts = 15;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                demo.run();
                System.out.println("[OK]");
                return;
            } catch (Throwable t) {
                if (attempt == maxAttempts) {
                    System.out.println("[FAIL] " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    return;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    static int envInt(String key, int def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : Integer.parseInt(v);
    }
}
