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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * "In Memory" key value storage, designed for debugging and test.
 */
public class InMemoryKeyValueStorage implements KeyValueStorage {

    private static final Object NULL = new Object();
    private final Node root = new Node("/");
    private final MessageBus<KvStorageEvent> bus;

    public InMemoryKeyValueStorage() {
        bus = MessageBuses.createConditional("inmemory", KvStorageEvent.class, KvStorageEvent::getKey, KvUtils::predicate);
    }

    @Override
    public String get(String key) {
        return root.get(key);
    }

    @Override
    public void set(String key, String value) {
        set(key, value, null);
    }

    @Override
    public void set(String key, String value, WriteOptions ops) {
        root.set(key, value);
    }

    @Override
    public void setdir(String key, WriteOptions ops) {
        root.setdir(key, ops);
    }

    @Override
    public void deletedir(String key, DeleteDirOptions ops) {
        root.deletedir(key, ops);
    }

    @Override
    public void delete(String key, WriteOptions ops) {
        root.delete(key, ops);
    }

    @Override
    public List<String> list(String key) {
        return root.list(key);
    }

    @Override
    public Map<String, String> map(String key) {
        return root.map(key);
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

        private <T> T doing(String key, boolean create, Function<String, T> onLeaf, BiFunction<String, Node, T> onNode) {
            int sp = key.indexOf('/');
            final int length = key.length();
            if(length > 1 && sp == length - 1) {
                key = key.substring(0, sp);
                sp = -1;
            }
            if(sp < 0) {
                return onLeaf.apply(key);
            }
            String dirname = key.substring(0, sp);
            Node node;
            if(create) {
                node = nodes.computeIfAbsent(dirname, Node::new);
            } else {
                node = nodes.get(dirname);
            }
            if(node == null) {
                return null;
            }
            return onNode.apply(key.substring(sp + 1), node);
        }

        String get(String key) {
            return doing(key, false,
              (k) -> {
                  Object val = leafs.get(k);
                  return val == NULL ? null : (String) val;
              },
              (k, dir) -> dir.get(k));
        }

        void set(String key, String value) {
            this.doing(key, true,
              (k) -> leafs.put(k, value == null? NULL : value),
              (k, dir) -> {
                  dir.set(k, value);
                  return null;
              });
        }

        void setdir(String key, WriteOptions ops) {
            this.<Object>doing(key, true,
              (k) -> nodes.put(k, new Node(k)),
              (k, dir) -> {
                  dir.setdir(k, ops);
                  return null;
              });
        }

        void deletedir(String key, DeleteDirOptions ops) {
            this.<Object>doing(key, false,
              nodes::remove,
              (k, dir) -> {
                  dir.deletedir(k, ops);
                  return null;
              });
        }

        void delete(String key, WriteOptions ops) {
            this.<Object>doing(key, false,
              leafs::remove,
              (k, dir) -> {
                  dir.delete(k, ops);
                  return null;
              });
        }

        public List<String> list(String key) {
            return doing(key, false,
              (k) -> {
                  Node node = nodes.get(k);
                  return node == null? Collections.emptyList() : new ArrayList<>(node.leafs.keySet());
              },
              (k, dir) -> dir.list(k));
        }

        public Map<String, String> map(String key) {
            return doing(key, false,
              (k) -> {
                  Node node = nodes.get(k);
                  Map<String, String> map = new HashMap<>();
                  if(node != null) {
                      node.leafs.forEach((lk, lv) -> {
                          map.put(lk, lv == NULL? null : (String) lv);
                      });
                  }
                  return map;
              },
              (k, dir) -> dir.map(k));
        }
    }
}
