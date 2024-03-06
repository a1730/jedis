package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.csc.hash.CommandLongHashing;
import redis.clients.jedis.csc.hash.OpenHftHashing;

public class CaffeineCSC extends ClientSideCache {

  private final Cache<Long, Object> cache;

  public CaffeineCSC(Cache<Long, Object> caffeineCache) {
    this(caffeineCache, new OpenHftHashing(OpenHftHashing.DEFAULT_HASH_FUNCTION), DefaultClientSideCacheable.INSTANCE);
  }

  public CaffeineCSC(Cache<Long, Object> caffeineCache, ClientSideCacheable cacheable) {
    this(caffeineCache, new OpenHftHashing(OpenHftHashing.DEFAULT_HASH_FUNCTION), cacheable);
  }

  public CaffeineCSC(Cache<Long, Object> caffeineCache, CommandLongHashing hashing, ClientSideCacheable cacheable) {
    super(hashing, cacheable);
    this.cache = caffeineCache;
  }

  @Override
  protected final void invalidateAllHashes() {
    cache.invalidateAll();
  }

  @Override
  protected void invalidateHashes(Iterable<Long> hashes) {
    cache.invalidateAll(hashes);
  }

  @Override
  protected void putValue(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object getValue(long hash) {
    return cache.getIfPresent(hash);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    // not using a default value to avoid an object creation like 'new OpenHftHashing(hashFunction)'
    private CommandLongHashing longHashing = null;

    private ClientSideCacheable cacheable = DefaultClientSideCacheable.INSTANCE;

    private Builder() { }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public Builder hashing(CommandLongHashing hashing) {
      this.longHashing = hashing;
      return this;
    }

    public Builder cacheable(ClientSideCacheable cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public CaffeineCSC build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return longHashing != null
          ? new CaffeineCSC(cb.build(), longHashing, cacheable)
          : new CaffeineCSC(cb.build(), cacheable);
    }
  }
}