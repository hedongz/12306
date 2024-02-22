/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengoofy.index12306.biz.orderservice.service.orderid;

import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.framework.starter.cache.DistributedCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 订单 ID 全局唯一生成器管理
 *
 */
@Component
@RequiredArgsConstructor
public final class OrderIdGeneratorManager implements InitializingBean {

    private final RedissonClient redissonClient;
    private final DistributedCache distributedCache;
    private static DistributedIdGenerator DISTRIBUTED_ID_GENERATOR;

    /**
     * 生成订单全局唯一 ID
     *
     * @param userId 用户名
     * @return 订单 ID
     */
    public static String generateId(long userId) {
        // 用DISTRIBUTED_ID_GENERATOR对象生成订单id + 用户id后六位
        // 得到一个基因算法
        return DISTRIBUTED_ID_GENERATOR.generateId() + String.valueOf(userId % 1000000);
    }

    // 在Bean属性设置好（注入）后，会执行。该方法可以用来执行一些在 Bean 属性设置完成后必须执行的初始化操作
    //
    // afterPropertiesSet 方法初始化一个分布式 ID 生成器，用于生成基于 userId 的唯一订单 ID。
    // 它使用 Redis 来管理和协调在多个节点或分片之间分发唯一的 ID 值，确保即使在分布式系统中，ID 仍然保持唯一。
    // incremented 变量跟踪用于 ID 生成的当前值。
    @Override
    public void afterPropertiesSet() throws Exception {
        String LOCK_KEY = "distributed_id_generator_lock_key";
        RLock lock = redissonClient.getLock(LOCK_KEY);
        lock.lock();
        try {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            String DISTRIBUTED_ID_GENERATOR_KEY = "distributed_id_generator_config";
            // 对 Redis 中某个 key 对应的值进行自增操作的方法。存在则自增，不存在则初始化为0。
            long incremented = Optional.ofNullable(instance.opsForValue().increment(DISTRIBUTED_ID_GENERATOR_KEY)).orElse(0L);
            // 注意：这里只是提供一种【分库分表基因法】的实现思路，所以将标识位定义 32。
            // 其次，如果对比 TB 网站订单号，应该不是在应用内生成，而是有一个全局服务调用获取

            int NODE_MAX = 32;
            // 检查增加的值是否大于预定义的最大值（NODE_MAX）。确定incremented是否超过了某个阈值。如果超过了，它会将值重置为0并在 Redis 中更新。
            if (incremented > NODE_MAX) {
                incremented = 0;
                instance.opsForValue().set(DISTRIBUTED_ID_GENERATOR_KEY, "0");
            }
            // 初始化 DISTRIBUTED_ID_GENERATOR这个对象
            DISTRIBUTED_ID_GENERATOR = new DistributedIdGenerator(incremented);
        } finally {
            lock.unlock();
        }
    }
}
