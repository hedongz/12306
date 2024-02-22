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

package org.opengoofy.index12306.biz.orderservice.dao.algorithm;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * 订单表相关复合分片算法配置
 *
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    @Getter
    private Properties props;

    private int shardingCount; // 分表数量，读取的配置中定义的分表数量

    private static final String SHARDING_COUNT_KEY = "sharding-count";

    /**
     * 订单表的复合分片算法
     * @param availableTargetNames 分片的集合，大小为16，从t_order_0到t_order_15
     * @param shardingValue 包含要分片的逻辑表名和分片关键词  含有——【逻辑表名：t_order】、【ColumnNameAndShardingValueMap：【order_sn ：value】、【user_id ：value】
     * @return
     */
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // columnNameAndShardingValuesMap存储
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();

        // 结果列表
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            String userId = "user_id";
            // 首先判断 SQL 是否包含用户 ID，如果包含直接取用户 ID 后六位
            Collection<Comparable<?>> customerUserIdCollection = columnNameAndShardingValuesMap.get(userId);
            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                // 获取到 SQL 中包含的用户 ID 对应值
                Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();
                // 如果使用 MybatisPlus 因为传入时没有强类型判断，所以有可能用户 ID 是字符串，也可能是 Long 等数值
                // 比如传入的用户 ID 可能是 1683025552364568576 也可能是 '1683025552364568576'
                // 根据不同的值类型，做出不同的获取后六位判断。字符串直接截取后六位，Long 类型直接通过 % 运算获取后六位
                if (comparable instanceof String) {
                    String actualUserId = comparable.toString();
                    result.add(shardingValue.getLogicTableName() + "_" + hashShardingValue(actualUserId.substring(Math.max(actualUserId.length() - 6, 0))) % shardingCount);
                } else {
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            } else {
                // 如果对订单中的 SQL 语句不包含用户 ID 那么就要从订单号中获取后六位，也就是用户 ID 后六位
                // 流程同用户 ID 获取流程
                String orderSn = "order_sn";
                Collection<Comparable<?>> orderSnCollection = columnNameAndShardingValuesMap.get(orderSn);
                Comparable<?> comparable = orderSnCollection.stream().findFirst().get();
                if (comparable instanceof String) {
                    // 取后六位，计算哈希值，然后取模，与逻辑表拼接，得到真正的表
                    String actualOrderSn = comparable.toString();
                    result.add(shardingValue.getLogicTableName() + "_" + hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0))) % shardingCount);
                } else {
                    String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);
                    result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
                }
            }
        }
        return result; // 返回的是表名
    }

    @Override
    public void init(Properties props) {
        this.props = props;
        shardingCount = getShardingCount(props);
    }

    private int getShardingCount(final Properties props) {
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}
