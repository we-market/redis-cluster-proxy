package cn.wemarket.redis.proxy.common.route;

import java.util.Collection;

public interface Router<T> {
    public void add(T node);

    public void add(Collection<T> nodes);

    public void remove(T node);

    public void remove(Collection<T> nodes);

    public T route(String key);

    public T spare(String key);
}
