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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.WithInterrupter;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.security.TempAuth;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
@Slf4j
class DockerMethodSubscriber<E, A extends WithInterrupter> implements LazySubscriptions.Subscriber<E> {

    @Data
    static class Builder<E, A extends WithInterrupter> {
        /**
         * String which identity resource, used for logging
         */
        private String id;
        private DockerService docker;
        private ExecutorService executorService;
        private Function<Consumer<E>, A> argument;
        private Function<A, ServiceCallResult> method;

        public Builder<E, A> argument(Function<Consumer<E>, A> argument) {
            setArgument(argument);
            return this;
        }

        public Builder<E, A> method(Function<A, ServiceCallResult> method) {
            setMethod(method);
            return this;
        }

        public Builder<E, A> id(String id) {
            setId(id);
            return this;
        }

        public Builder<E, A> docker(DockerService service) {
            setDocker(service);
            return this;
        }

        public Builder<E, A> executorService(ExecutorService executorService) {
            setExecutorService(executorService);
            return this;
        }

        public DockerMethodSubscriber<E, A> build() {
            return new DockerMethodSubscriber<>(this);
        }
    }

    private final String id;
    private final ExecutorService executorService;
    private final Function<Consumer<E>, A> argument;
    private final Function<A, ServiceCallResult> method;

    private DockerMethodSubscriber(Builder<E, A> b) {
        this.id = b.id;
        this.argument = b.argument;
        this.executorService = b.executorService;
        this.method = b.method;
    }

    public static <E, A extends WithInterrupter> Builder<E, A> builder() {
        return new Builder<E, A>();
    }

    @Override
    public Runnable subscribe(LazySubscriptions<E>.Context context) {
        A arg = argument.apply(context::accept);
        Future<ServiceCallResult> future = executorService.submit(() -> {
            //here we use sys auth because it shared between different users
            // may be we need to use different subscriptions for each users?
            try (TempAuth ta = TempAuth.asSystem()) {
                return method.apply(arg);
            } finally {
                context.close();
            }
        });
        try {
            ServiceCallResult result = future.get(2, TimeUnit.MILLISECONDS);
            if(result.getCode() != ResultCode.OK) {
                log.warn("Can not subscribe on id=\"{}\" due error {}: {}", id, result.getCode(), result.getMessage());
            }
        } catch (TimeoutException e) {
            //it is ok
        } catch (Exception e) {
            log.warn("Can not subscribe on id=\"{}\" due error", id, e);
        }
        return () -> {
            arg.getInterrupter().set(true);
            future.cancel(true);
        };
    }
}
