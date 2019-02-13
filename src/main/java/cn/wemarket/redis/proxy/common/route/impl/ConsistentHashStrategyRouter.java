package cn.wemarket.redis.proxy.common.route.impl;

import cn.wemarket.redis.proxy.common.route.Router;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConsistentHashStrategyRouter<T> implements Router<T> {
    private final HashFunction hashFunction = Hashing.murmur3_128();
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = reentrantReadWriteLock.readLock();
    private final Lock writeLock = reentrantReadWriteLock.writeLock();

    private int numberOfReplicas;
    private SortedMap<Long, T> hashCircle = new TreeMap<>();

    public ConsistentHashStrategyRouter(int numberOfReplicas, Collection<T> nodes){
        this.numberOfReplicas  = numberOfReplicas;
        add(nodes);
    }

    @Override
    public void add(T node) {
        writeLock.lock();
        try {
            addNode(node);
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(Collection<T> nodes) {
        writeLock.lock();
        try {
            for (T node:nodes) {
                addNode(node);
            }
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(T node) {
        readLock.lock();
        try {
            removeNode(node);
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public void remove(Collection<T> nodes) {
        readLock.lock();
        try {
            for (T node:nodes) {
                removeNode(node);
            }
        }finally {
            readLock.unlock();
        }
    }

    //哈希循环
    @Override
    public T route(String key) {
        if (CollectionUtils.isEmpty(hashCircle)){
            return null;
        }

        long hash = getHash(key);
        readLock.lock();
        try {
            if (!hashCircle.containsKey(hash)){
                SortedMap<Long, T> tailMap = hashCircle.tailMap(hash);
                hash = tailMap.isEmpty()?hashCircle.firstKey():tailMap.firstKey();
            }

            return hashCircle.get(hash);
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public T spare(String key) {
        return null;
    }

    private void addNode(T node){
        for (int i = 0; i < numberOfReplicas / 4; i++) {
            byte[] digest = md5(node.toString()+"-"+i);
            for (int j = 0; j < 4; j++) {
                hashCircle.put(getHash(digest, j), node);
            }
        }
    }

    private void removeNode(T node){
        for (int i = 0; i < numberOfReplicas / 4; i++) {
            byte[] digest = md5(node.toString()+"-"+i);
            for (int j = 0; j < 4; j++) {
                hashCircle.remove(getHash(digest, j), node);
            }
        }
    }

    private static byte[] md5(String text){
        return Hashing.md5().hashBytes(text.getBytes()).asBytes();
    }

    private static long getHash(final String key){
        byte[] digest = md5(key);
        return getHash(digest, 0) & 0xFFFFFFFFFL;
    }

    private static long getHash(byte[] digest, int h){
        return ((long)(digest[3+h+4] & 0xFF) << 24) | ((long)(digest[2+h+4] & 0xFF) << 16) | ((long)(digest[1+h+4] & 0xFF) << 8) | ((long)(digest[h+4] & 0xFF));
    }

    public SortedMap<Long, T> getHashCircle(){
        return hashCircle;
    }

    public void setHashCircle(SortedMap<Long, T> hashCircle){
        this.hashCircle = hashCircle;
    }
}
