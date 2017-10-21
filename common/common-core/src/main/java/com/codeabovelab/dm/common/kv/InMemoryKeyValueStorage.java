/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.kv;

import com.codeabovelab.dm.common.mb.ConditionalSubscriptions;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBuses;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * "In Memory" key value storage, designed for debugging and test.
 */
public class InMemoryKeyValueStorage implements KeyValueStorage {

    @Data
    public static class Builder {

        /**
         * Override default single thread executor of this storage events.
         */
        private Executor eventsExecutor;

        /**
         * Override default single thread executor of this storage events.
         * @param eventsExecutor executor instance or null
         * @return this
         */
        public Builder eventsExecutor(Executor eventsExecutor) {
            setEventsExecutor(eventsExecutor);
            return this;
        }

        public InMemoryKeyValueStorage build() {
            return new InMemoryKeyValueStorage(this);
        }
    }

    private class Context {
        private final String key;
        private int pos;
        private String current;
        private boolean leaf;

        Context(String key) {
            this.key = key;
            pos = key.charAt(0) == '/' ? 1 : 0;
        }

        private void fire(long index, KvStorageEvent.Crud action, String val) {
            KvStorageEvent e = new KvStorageEvent(index, key, val, Long.MAX_VALUE, action);
            executor.execute(() -> bus.accept(e));
        }

        public void next() {
            int sp = key.indexOf('/', pos);
            final int length = key.length();
            leaf = (length > 1 && sp == length - 1) || sp == -1;
            if(sp < 0) {
                current = key.substring(pos);
            } else {
                current = key.substring(pos, sp);
            }
            if(!leaf) {
                pos = sp + 1; // pos after '/'
            }

        }

        boolean isLeaf() {
            return leaf;
        }
    }
    private static final Object NULL = new Object();
    private final Node root = new Node("/");
    private final MessageBus<KvStorageEvent> bus;
    private final AtomicInteger counter = new AtomicInteger();
    private final Executor executor;

    public InMemoryKeyValueStorage() {
        this(builder());
    }

    public InMemoryKeyValueStorage(Builder builder) {
        Executor ex = builder.eventsExecutor;
        if(ex == null) {
            ex = ExecutorUtils.DIRECT;
        }
        this.executor = ex;
        bus = MessageBuses.createConditional("inmemory", KvStorageEvent.class, KvStorageEvent::getKey, KvUtils::predicate);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public KvNode get(String key) {
        return root.get(new Context(key));
    }

    @Override
    public KvNode set(String key, String value) {
        return set(key, value, null);
    }

    @Override
    public KvNode set(String key, String value, WriteOptions ops) {
        return root.set(new Context(key), value);
    }

    @Override
    public KvNode setdir(String key, WriteOptions ops) {
        return root.setdir(new Context(key), ops);
    }

    @Override
    public KvNode deletedir(String key, DeleteDirOptions ops) {
        return root.deletedir(new Context(key), ops);
    }

    @Override
    public KvNode delete(String key, WriteOptions ops) {
        return root.delete(new Context(key), ops);
    }

    @Override
    public List<String> list(String key) {
        return root.list(new Context(key));
    }

    @Override
    public Map<String, String> map(String key) {
        return root.map(new Context(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ConditionalSubscriptions<KvStorageEvent, String> subscriptions() {
        return (ConditionalSubscriptions<KvStorageEvent, String>) bus.asSubscriptions();
    }

    @Override
    public String getPrefix() {
        return "dmp";
    }

    private class Node {
        private final String path;
        private final ConcurrentMap<String, Object> nodes = new ConcurrentHashMap<>();
        private volatile long index;

        Node(String path) {
            this.path = path;
        }

        private synchronized <T> T doing(Context ctx, boolean create, Function<Context, T> onLeaf, BiFunction<Context, Node, T> onNode) {
            ctx.next();
            if(ctx.isLeaf()) {
                return onLeaf.apply(ctx);
            }
            String dirname = ctx.current;
            Object node;
            if(create) {
                node = nodes.computeIfAbsent(dirname, Node::new);
            } else {
                node = nodes.get(dirname);
            }
            if(node == null) {
                return null;
            }
            assertNode(ctx, node);
            return onNode.apply(ctx, (Node) node);
        }

        private void assertNode(Context ctx, Object obj) {
            if(!(obj instanceof Node)) {
                throw new RuntimeException("The " + ctx.current + " in " + ctx.key + " is not a directory.");
            }
        }

        private void assertNullOrNode(Context ctx) {
            Object o = nodes.get(ctx.current);
            assertNullOrNode(ctx, o);
        }

        private void assertNullOrNode(Context ctx, Object o) {
            if(o == null) {
                return;
            }
            assertNode(ctx, o);
        }

        private void assertNullOrNotNode(Context ctx) {
            Object o = nodes.get(ctx.current);
            if(o instanceof Node) {
                throw new RuntimeException("The " + ctx.current + " in " + ctx.key + " is a directory.");
            }
        }

        KvNode get(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Object val = nodes.get(k.current);
                  if(val == null) {
                      return null;
                  }
                  if(val instanceof Node) {
                      return KvNode.dir(index);
                  }
                  String strVal = toStrVal(val);
                  ctx.fire(index, KvStorageEvent.Crud.READ, strVal);
                  return toNode(strVal);
              },
              (k, dir) -> dir.get(k));
        }

        KvNode set(Context ctx, String value) {
            return doing(ctx, true,
              (k) -> {
                  assertNullOrNotNode(k);
                  Object old = nodes.put(k.current, value == null? NULL : value);
                  String strVal = toStrVal(value);
                  index++;
                  ctx.fire(index, old == null? KvStorageEvent.Crud.CREATE : KvStorageEvent.Crud.UPDATE, strVal);
                  return toNode(strVal);
              },
              (k, dir) -> dir.set(k, value));
        }

        KvNode setdir(Context ctx, WriteOptions ops) {
            return doing(ctx, true,
              (k) -> {
                  assertNullOrNode(k);
                  Object old = nodes.putIfAbsent(k.current, new Node(k.current));
                  if(old == null) {
                      index++;
                      ctx.fire(index, KvStorageEvent.Crud.CREATE, null);
                  }
                  return toNode(null);
              },
              (k, dir) -> dir.setdir(k, ops));
        }

        KvNode deletedir(Context ctx, DeleteDirOptions ops) {
            return doing(ctx, false,
              (k) -> {
                  assertNullOrNode(k);
                  Node old = (Node) nodes.remove(k.current);
                  if(old != null) {
                      index++;
                      ctx.fire(index, KvStorageEvent.Crud.DELETE, null);
                  }
                  return toNode(null);
              },
              (k, dir) -> dir.deletedir(k, ops));
        }

        private KvNode toNode(String val) {
            return KvNode.leaf(index, val);
        }

        KvNode delete(Context ctx, WriteOptions ops) {
            return doing(ctx, false,
              (k) -> {
                  assertNullOrNotNode(k);
                  Object old = nodes.remove(k.current);
                  String val = toStrVal(old);
                  if(old != null) {
                      index++;
                      ctx.fire(index, KvStorageEvent.Crud.DELETE, val);
                  }
                  return toNode(val);
              },
              (k, dir) -> dir.delete(k, ops));
        }

        public List<String> list(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Object o = nodes.get(k.current);
                  assertNullOrNode(k, o);
                  Node node = (Node) o;
                  return node == null? Collections.emptyList() : new ArrayList<>(node.nodes.keySet());
              },
              (k, dir) -> dir.list(k));
        }

        public Map<String, String> map(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Object o = nodes.get(k.current);
                  Map<String, String> map = new HashMap<>();
                  if(o != null) {
                      assertNode(k, o);
                      Node node = (Node) o;
                      ctx.fire(index, KvStorageEvent.Crud.READ, null);
                      node.nodes.forEach((lk, lv) -> {
                          String str = lv instanceof Node ? null : toStrVal(lv);
                          map.put(lk, str);
                      });
                  }
                  return map;
              },
              (k, dir) -> dir.map(k));
        }
    }

    private String toStrVal(Object val) {
        if(val instanceof Node) {
            throw new IllegalArgumentException("Can not convert node to string.");
        }
        return val == NULL ? null : (String) val;
    }
}
