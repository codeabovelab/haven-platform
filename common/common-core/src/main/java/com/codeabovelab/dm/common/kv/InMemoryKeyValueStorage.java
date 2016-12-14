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

import com.codeabovelab.dm.common.mb.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * "In Memory" key value storage, designed for debugging and test.
 */
public class InMemoryKeyValueStorage implements KeyValueStorage {
    private class Context {
        private final String key;
        private int pos;
        private String current;
        private boolean leaf;

        Context(String key) {
            this.key = key;
            pos = key.charAt(0) == '/' ? 1 : 0;
        }

        private void fire(KvStorageEvent.Crud action, String val) {
            bus.accept(new KvStorageEvent(counter.incrementAndGet(), key, val, Long.MAX_VALUE, action));
        }

        public void next() {
            int sp = key.indexOf('/', pos);
            final int length = key.length();
            leaf = (length > 1 && sp == length - 1) || sp == -1;
            if(leaf) {
                current = key;
            } else {
                current = key.substring(pos, sp);
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

    public InMemoryKeyValueStorage() {
        bus = MessageBuses.createConditional("inmemory", KvStorageEvent.class, KvStorageEvent::getKey, KvUtils::predicate);
    }


    @Override
    public String get(String key) {
        return root.get(new Context(key));
    }

    @Override
    public void set(String key, String value) {
        set(key, value, null);
    }

    @Override
    public void set(String key, String value, WriteOptions ops) {
        root.set(new Context(key), value);
    }

    @Override
    public void setdir(String key, WriteOptions ops) {
        root.setdir(new Context(key), ops);
    }

    @Override
    public void deletedir(String key, DeleteDirOptions ops) {
        root.deletedir(new Context(key), ops);
    }

    @Override
    public void delete(String key, WriteOptions ops) {
        root.delete(new Context(key), ops);
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
    public String getDockMasterPrefix() {
        return "dmp";
    }

    private class Node {
        private final String path;
        private final ConcurrentMap<String, Node> nodes = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Object> leafs = new ConcurrentHashMap<>();

        Node(String path) {
            this.path = path;
        }

        private <T> T doing(Context ctx, boolean create, Function<Context, T> onLeaf, BiFunction<Context, Node, T> onNode) {
            ctx.next();
            if(ctx.isLeaf()) {
                return onLeaf.apply(ctx);
            }
            String dirname = ctx.current;
            Node node;
            if(create) {
                node = nodes.computeIfAbsent(dirname, Node::new);
            } else {
                node = nodes.get(dirname);
            }
            if(node == null) {
                return null;
            }
            return onNode.apply(ctx, node);
        }

        String get(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Object val = leafs.get(k.current);
                  String strVal = toStrVal(val);
                  ctx.fire(KvStorageEvent.Crud.READ, strVal);
                  return strVal;
              },
              (k, dir) -> dir.get(k));
        }

        void set(Context ctx, String value) {
            this.doing(ctx, true,
              (k) -> {
                  Object old = leafs.put(k.current, value == null? NULL : value);
                  ctx.fire(old == null? KvStorageEvent.Crud.CREATE : KvStorageEvent.Crud.UPDATE, toStrVal(value));
                  return old;
              },
              (k, dir) -> {
                  dir.set(k, value);
                  return null;
              });
        }

        void setdir(Context ctx, WriteOptions ops) {
            this.<Object>doing(ctx, true,
              (k) -> {
                  Node old = nodes.put(k.current, new Node(k.current));
                  ctx.fire(KvStorageEvent.Crud.CREATE, null);
                  return old;
              },
              (k, dir) -> {
                  dir.setdir(k, ops);
                  return null;
              });
        }

        void deletedir(Context ctx, DeleteDirOptions ops) {
            this.<Object>doing(ctx, false,
              (k) -> {
                  Node old = nodes.remove(k.current);
                  ctx.fire(KvStorageEvent.Crud.DELETE, null);
                  return old;
              },
              (k, dir) -> {
                  dir.deletedir(k, ops);
                  return null;
              });
        }

        void delete(Context ctx, WriteOptions ops) {
            this.<Object>doing(ctx, false,
              (k) -> {
                  Object old = leafs.remove(k.current);
                  ctx.fire(KvStorageEvent.Crud.DELETE, toStrVal(old));
                  return old;
              },
              (k, dir) -> {
                  dir.delete(k, ops);
                  return null;
              });
        }

        public List<String> list(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Node node = nodes.get(k.current);
                  return node == null? Collections.emptyList() : new ArrayList<>(node.leafs.keySet());
              },
              (k, dir) -> dir.list(k));
        }

        public Map<String, String> map(Context ctx) {
            return doing(ctx, false,
              (k) -> {
                  Node node = nodes.get(k.current);
                  Map<String, String> map = new HashMap<>();
                  if(node != null) {
                      ctx.fire(KvStorageEvent.Crud.READ, null);
                      node.leafs.forEach((lk, lv) -> {
                          map.put(lk, toStrVal(lv));
                      });
                  }
                  return map;
              },
              (k, dir) -> dir.map(k));
        }
    }

    private String toStrVal(Object val) {
        return val == NULL ? null : (String) val;
    }
}
