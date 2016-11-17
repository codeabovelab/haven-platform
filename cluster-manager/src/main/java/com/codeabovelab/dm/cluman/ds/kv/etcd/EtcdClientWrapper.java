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

package com.codeabovelab.dm.cluman.ds.kv.etcd;

import com.codeabovelab.dm.common.kv.*;
import com.codeabovelab.dm.common.mb.*;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyDeleteRequest;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class EtcdClientWrapper implements KeyValueStorage {
    private static final int KEY_NOT_FOUND = 100;
    private static final int NOT_A_FILE = 102;
    private static final int KEY_ALREADY_EXISTS = 105;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EtcdClient etcd;
    private final String prefix;
    private final MessageBus<KvStorageEvent> bus;
    private final ExecutorService executor;

    public EtcdClientWrapper(EtcdClient etcd, String prefix) {
        this.etcd = etcd;
        this.prefix = prefix;
        //possibly we need to create better id ob bus
        this.bus = MessageBusImpl.builder(KvStorageEvent.class, (s) -> new ConditionalMessageBusWrapper<>(s, KvStorageEvent::getKey, KvUtils::predicate))
          .id(getClass().getName())
          .build();
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setNameFormat(getClass().getName() + "-bus-%d")
          .setDaemon(true)
          .build());
        eventWhirligig(-1);
    }



    private void eventWhirligig(final long index) {
        try {
            // getAll() - not working
            EtcdKeyGetRequest req = this.etcd.get("").recursive();
            if(index > 0) {
                req.waitForChange(index);
            } else {
                req.waitForChange();
            }
            EtcdResponsePromise<EtcdKeysResponse> promise = req.send();
            final boolean debug = log.isDebugEnabled();
            promise.addListener(rp -> {
                try {
                    EtcdKeysResponse r = rp.get();
                    //immediate subscribe for next events
                    eventWhirligig(r.node.modifiedIndex + 1);
                    //in future we must to remove this logging, but not now
                    if(debug) {
                        log.debug("{} {}={} (ttl:{}) {}", r.etcdIndex, r.node.key, r.node.value, r.node.ttl, r.action);
                    }

                    KvStorageEvent.Crud action = null;
                    switch (r.action) {
                        case compareAndDelete:
                        case delete:
                        case expire:
                            action = KvStorageEvent.Crud.DELETE;
                            break;
                        case create:
                            action = KvStorageEvent.Crud.CREATE;
                            break;
                        case compareAndSwap:
                        case set:
                        case update:
                            action = KvStorageEvent.Crud.UPDATE;
                            break;
                    }
                    if(action != null) {
                        KvStorageEvent e = new KvStorageEvent(r.etcdIndex, r.node.key, r.node.value, r.node.ttl, action);
                        this.executor.submit(() -> bus.accept(e));
                    }
                } catch (Exception e) {
                    logger.error("Error when process event response", e);
                }
            });
        } catch (Exception e) {
            logger.error("Error when process events", e);
        }
    }

    @Override
    public String get(String key) {
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = etcd.get(key).send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            String value = etcdKeysResponse.node.value;
            log.debug("get value {} for key {}", etcdKeysResponse.node.value, etcdKeysResponse.node.key);
            return value;
        } catch (EtcdException e) {
            if (e.errorCode != KEY_NOT_FOUND) {
                log.error("Error during fetching key", e);
            }
            return null;
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public void set(String key, String value) {
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = etcd.put(key, value).send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            log.debug("set value {} for key {}", etcdKeysResponse.node.value, etcdKeysResponse.node.key);
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }


    @Override
    public void set(String key, String value, WriteOptions ops) {
        EtcdKeyPutRequest req = etcd.put(key, value);
        fillPutReq(ops, req);
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = req.send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            log.debug("set value {} for key {}, ops {}", etcdKeysResponse.node.value, etcdKeysResponse.node.key, ops);
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }

    }


    @Override
    public void delete(String key, WriteOptions ops) {
        EtcdKeyDeleteRequest req = etcd.delete(key);
        fillDeleteReq(ops, req);
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = req.send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            log.debug("deleted key {}", etcdKeysResponse.node.key);
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    private void fillPutReq(WriteOptions ops, EtcdKeyPutRequest req) {
        if(ops == null) {
            return;
        }
        // we hope (due to check in WriteOptions ctor) that both flags can not be set simultaneous
        if(ops.isFailIfAbsent() || ops.isFailIfExists()) {
            req.prevExist(ops.isFailIfAbsent());
        }
        final long ttl = ops.getTtl();
        if(ttl > 0) {
            if(ttl >= Integer.MAX_VALUE) {
                throw new IllegalArgumentException("TTL value too big: " + ttl);
            }
            req.ttl((int) ttl);
        }
        int prevIndex = ops.getPrevIndex();
        if(prevIndex > 0) {
            req.prevIndex(prevIndex);
        }
    }

    private void fillDeleteReq(WriteOptions ops, EtcdKeyDeleteRequest req) {
        if(ops == null) {
            return;
        }
        final int prevIndex = ops.getPrevIndex();
        if(prevIndex > 0) {
            req.prevIndex(prevIndex);
        }
    }

    @Override
    public void setdir(String key, WriteOptions ops) {
        EtcdKeyPutRequest req = etcd.putDir(key);
        fillPutReq(ops, req);
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = req.send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            log.debug("make dir at key {}", etcdKeysResponse.node.key);
        } catch (EtcdException e) {
            if(e.errorCode == NOT_A_FILE /* not a file */ || e.errorCode == KEY_ALREADY_EXISTS) {
                // https://github.com/coreos/etcd/issues/169
                //it usually mean that dir is exists, code depend of request flag
                if(ops.isFailIfExists()) {
                    throw new RuntimeException(key + " already exists.", e);
                }
            } else {
                throw Throwables.asRuntime(e);
            }
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public void deletedir(String key, DeleteDirOptions ops) {
        EtcdKeyDeleteRequest req = etcd.deleteDir(key);
        if(ops.isRecursive()) {
            req.recursive();
        }
        fillDeleteReq(ops, req);
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = req.send();
            EtcdKeysResponse etcdKeysResponse = send.get();
            log.debug("deleted key {}", etcdKeysResponse.node.key);
        } catch (EtcdException e) {
            if(e.errorCode != KEY_NOT_FOUND || ops.isFailIfAbsent()) {
                throw Throwables.asRuntime(e);
            }
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public List<String> list(String key) {
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = etcd.getDir(key).send();
            EtcdKeysResponse r = send.get();
            return r.node.nodes.stream().map(n -> n.key).collect(Collectors.toList());
        } catch (EtcdException e) {
            if(e.getErrorCode() == KEY_NOT_FOUND) {
                return null;
            }
            throw Throwables.asRuntime(e);
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public Map<String, String> map(String key) {
        try {
            EtcdResponsePromise<EtcdKeysResponse> send = etcd.get(key).recursive().send();
            EtcdKeysResponse r = send.get();
            return r.node.nodes.stream().collect(Collectors.toMap((n) -> n.key, (n) -> n.value));
        } catch (EtcdException e) {
            if (e.errorCode != KEY_NOT_FOUND) {
                log.error("Error during fetching key", e);
            }
            //we need to have differences between empty map and absent key (or errors)
            return null;
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConditionalSubscriptions<KvStorageEvent, String> subscriptions() {
        return (ConditionalSubscriptions<KvStorageEvent, String>) bus.asSubscriptions();
    }

    @Override
    public String getDockMasterPrefix() {
        return prefix;
    }
}
