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

package org.opengoofy.index12306.biz.userservice.service.handler.filter.user;

import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import org.opengoofy.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import org.opengoofy.index12306.biz.userservice.service.UserLoginService;
import org.opengoofy.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * [用户注册用户名唯一检验]
 *
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserLoginService userLoginService;

    /**
     * 验证用户名是否可用
     * @param requestParam 责任链执行入参
     */
    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        // 如果从redis中查到用户名不存在，说明用户名没有被注销，不可以使用！
        if (!userLoginService.hasUsername(requestParam.getUsername())) {
            throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL); // 用户名已存在
        }
    }

    @Override
    public int getOrder() {
        return 1;  // 优先处理用户注册，判断是否存在，因为判空的逻辑处理较快！
    }
}
